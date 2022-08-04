package com.example.cx_pose_capture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.impl.utils.PreferenceUtils;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.cx_pose_capture.databinding.ActivityRecordingBinding;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.odml.image.MediaMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class RecordingActivity extends AppCompatActivity {
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    BufferedWriter writer;
    private DataWritingThread mDataWriteThread;
    private Handler mDataWriteHandler;

    private boolean showInFrameLikelihood = true;
    private boolean visualizeZ = true;
    private boolean rescaleZForVisualization = true;
    private List<String> fauxPoseClassificationResult = Arrays.asList("A Pose");

    private SurfaceView surfaceView;

    private ActivityRecordingBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        Button buttonView = findViewById(R.id.btn_EndRecording);
        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialAlertDialogBuilder(RecordingActivity.this)
                        .setTitle(getString(R.string.dialog_EndRecording_Title))
                        .setMessage(getString(R.string.dialog_EndRecording_Msg))
                        .setNeutralButton(getString(R.string.dialog_EndRecording_Cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing
                            }
                        })
                        .setPositiveButton(getString(R.string.dialog_EndRecording_Ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                endRecording();
                            }
                        }).show();

            }
        });

        mDataWriteThread = new DataWritingThread("DataWritingThread");
        mDataWriteThread.start();
        mDataWriteHandler = mDataWriteThread.getHandler();

        surfaceView = new SurfaceView(this);
        previewView = findViewById(R.id.preview_Recording);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));



    }





    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {


        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        Preview preview = new Preview.Builder().build();

        Preview previewPose = new Preview.Builder().build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();


        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {

                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {

                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);


                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay.setImageSourceInfo(
                                imageProxy.getWidth(), imageProxy.getHeight(), true);
                    } else {
                        graphicOverlay.setImageSourceInfo(
                                imageProxy.getHeight(), imageProxy.getWidth(), true);
                    }

                    Task<Pose> result = pose_estimation(image);
                    result.addOnCompleteListener(results -> imageProxy.close());



                }
            }
        });



        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);




    }

    private Task<Pose> pose_estimation(InputImage image){

        AccuratePoseDetectorOptions options_PoseDetector = new AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build();
       PoseDetector poseDetector = PoseDetection.getClient(options_PoseDetector);



        Task<Pose> result =
                poseDetector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<Pose>() {
                                    @Override
                                    public void onSuccess(Pose pose) {
                                        Log.d("EXPECTED", "Processes image successfully");


                                        mDataWriteHandler.post(new Runnable() {
                                            @Override
                                            public void run() {

                                                mDataWriteThread.writePoseToFile(pose);
                                            }
                                        });

                                        graphicOverlay.getContext();
                                        graphicOverlay.clear();
                                        graphicOverlay.add(
                                                new PoseGraphic(
                                                        graphicOverlay,
                                                        pose,
                                                        showInFrameLikelihood,
                                                        visualizeZ,
                                                        rescaleZForVisualization,
                                                        fauxPoseClassificationResult));

                                        graphicOverlay.postInvalidate();


                                    }
                                })
                            .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("EXPECTED", e.getMessage());
                                    }
                                });

        return result;
    }



    public void endRecording() {
        // Do something in response to button
        Log.d("EXPECTED", "User has ended recording");

        this.finish();


    }

}

