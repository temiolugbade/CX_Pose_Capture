package com.example.cx_pose_capture;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.posedetector.PoseGraphic;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;


public class RecordingActivity extends AppCompatActivity {


    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private CheckBox chkShowInFrameLikelihood;
    private CheckBox chkVisualizeZ;
    private TextView txtAffectPrediction;

    private List<String> poseclassificationResult = Arrays.asList("faux pose class");


    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    private DataWritingThread mDataWriteThread;
    private Handler mDataWriteHandler;
    private AffectPredictionThread mAffectPredThread;
    private Handler mAffectPredHandler;

    private boolean showInFrameLikelihood;
    private boolean visualizeZ;
    private boolean rescaleZForVisualization = true;






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
        mDataWriteHandler = mDataWriteThread.extraSetup(DataWritingThread.createWriteFolder(getApplicationContext()));


        mAffectPredThread = new AffectPredictionThread("AffectPredictionThread");
        mAffectPredThread.start();
        mAffectPredHandler = mAffectPredThread.getHandler();


        previewView = findViewById(R.id.preview_Recording);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        chkShowInFrameLikelihood = findViewById(R.id.chk_showInFrameLikelihood);
        chkVisualizeZ = findViewById(R.id.chk_visualizeZ);
        txtAffectPrediction = findViewById(R.id.txt_PoseClassification);

        showInFrameLikelihood = chkShowInFrameLikelihood.isChecked();
        chkShowInFrameLikelihood.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                showInFrameLikelihood = chkShowInFrameLikelihood.isChecked();

            }
        });

        visualizeZ = chkVisualizeZ.isChecked();
        chkVisualizeZ.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                visualizeZ = chkVisualizeZ.isChecked();

            }
        });

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


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();




        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //ViewPort viewPort = previewView.getViewPort();



        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @SuppressLint("RestrictedApi")
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {

                imageProxy.setCropRect(preview.getViewPortCropRect());



                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {

                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);


                    Matrix mappingmatrix = getMappingMatrix(imageProxy, previewView);

                    //boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                    boolean isImageFlipped = true;
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay.setImageSourceInfo(
                                imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped, mappingmatrix);
                    } else {
                        graphicOverlay.setImageSourceInfo(
                                imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped, mappingmatrix);
                    }



                    Task<Pose> result = pose_estimation(image);
                    result.addOnCompleteListener(results -> imageProxy.close());



                }
            }
        });



        /*UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .build();*/

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);

        //cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, useCaseGroup);


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

                                        graphicOverlay.clear();
                                        graphicOverlay.add(
                                                new PoseGraphic(
                                                        graphicOverlay,
                                                        pose,
                                                        showInFrameLikelihood,
                                                        visualizeZ,
                                                        rescaleZForVisualization));

                                        mAffectPredHandler.post(new Runnable() {
                                            @Override
                                            public void run() {

                                                mAffectPredThread.predictAffect(pose, txtAffectPrediction);
                                            }
                                        });



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

    Matrix getMappingMatrix(ImageProxy imageProxy, PreviewView previewView) {
        Rect cropRect = imageProxy.getCropRect();
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        Matrix matrix = new Matrix();

        // A float array of the source vertices (crop rect) in clockwise order.
        float[] source = {
                cropRect.left,
                cropRect.top,
                cropRect.right,
                cropRect.top,
                cropRect.right,
                cropRect.bottom,
                cropRect.left,
                cropRect.bottom
        };

        // A float array of the destination vertices in clockwise order.
        float[] destination = {
                0f,
                0f,
                previewView.getWidth(),
                0f,
                previewView.getWidth(),
                previewView.getHeight(),
                0f,
                previewView.getHeight()
        };

        // The destination vertexes need to be shifted based on rotation degrees.
        // The rotation degree represents the clockwise rotation needed to correct
        // the image.

        // Each vertex is represented by 2 float numbers in the vertices array.
        int vertexSize = 2;
        // The destination needs to be shifted 1 vertex for every 90° rotation.
        int shiftOffset = rotationDegrees / 90 * vertexSize;
        float[] tempArray = destination.clone();
        for (int toIndex = 0; toIndex < source.length; toIndex++) {
            int fromIndex = (toIndex + shiftOffset) % source.length;
            destination[toIndex] = tempArray[fromIndex];
        }
        matrix.setPolyToPoly(source, 0, destination, 0, 4);
        return matrix;
    }

    public void endRecording() {
        // Do something in response to button
        Log.d("EXPECTED", "User has ended recording");

        this.finish();


    }

}

