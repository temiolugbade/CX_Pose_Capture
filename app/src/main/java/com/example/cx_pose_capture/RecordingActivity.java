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

import android.annotation.SuppressLint;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class RecordingActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    BufferedWriter writer;
    private DataWritingThread mThread;
    private Handler mHandler;


    private final int[] POSE_LANDMARKS = {PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE, PoseLandmark.LEFT_PINKY, PoseLandmark.RIGHT_PINKY,
            PoseLandmark.LEFT_INDEX, PoseLandmark.RIGHT_INDEX, PoseLandmark.LEFT_THUMB, PoseLandmark.RIGHT_THUMB,
            PoseLandmark.LEFT_HEEL, PoseLandmark.RIGHT_HEEL, PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.RIGHT_FOOT_INDEX,
            PoseLandmark.NOSE, PoseLandmark.LEFT_EYE_INNER, PoseLandmark.LEFT_EYE, PoseLandmark.LEFT_EYE_OUTER,
            PoseLandmark.RIGHT_EYE_INNER, PoseLandmark.RIGHT_EYE, PoseLandmark.RIGHT_EYE_OUTER,
            PoseLandmark.LEFT_EAR, PoseLandmark.RIGHT_EAR, PoseLandmark.LEFT_MOUTH, PoseLandmark.RIGHT_MOUTH};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        Button buttonView = findViewById(R.id.btn_EndRecording);
        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endRecording();
            }
        });

        mThread = new DataWritingThread("DataWritingThread");
        mThread.start();
        mHandler = mThread.getHandler();

        createWriteFolder();

        previewView = findViewById(R.id.preview_Recording);
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


    private void createWriteFolder(){
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            //fail safely
            Log.d("EXPECTED", "I CANNOT write to storage");
        }else{

            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    getString(R.string.storage_directory));

            if (!dir.mkdirs()) {
                if(!dir.exists()) {
                    Log.d("EXPECTED", "Could NOT create directory");
                }
            }

            File file = new File(dir, getString(R.string.storage_directory)+" "+getDateTime()+".txt");
            try {
                writer = new BufferedWriter(new FileWriter(file));
            }catch(IOException e){
                Log.d("EXPECTED", "Writer NOT created");
            }



        }

    }

    private String getDateTime(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_SSS");
        return simpleDateFormat.format(calendar.getTime()).toString();
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

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


                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo()
                                                .getRotationDegrees());
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
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {

                                                writePoseToFile(pose);
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


    private void writePoseToFile(Pose pose){

        // Get all PoseLandmarks. If no person was detected, the list will be empty
        List<PoseLandmark> allPoseLandmarks = pose.getAllPoseLandmarks();

        if (!allPoseLandmarks.isEmpty()){

            // Or get specific PoseLandmarks individually. These will all be null if no person
            // was detected
            float[][] landmark_Pos_Conf = new float[4][POSE_LANDMARKS.length];
            PointF3D landmark_Pos3D;
            for (int landmark : POSE_LANDMARKS) {
                landmark_Pos3D = pose.getPoseLandmark(landmark).getPosition3D();
                landmark_Pos_Conf[0][landmark] = landmark_Pos3D.getX();
                landmark_Pos_Conf[1][landmark] = landmark_Pos3D.getY();
                landmark_Pos_Conf[2][landmark] = landmark_Pos3D.getZ();
                landmark_Pos_Conf[3][landmark] = pose.getPoseLandmark(landmark).getInFrameLikelihood();


                Log.d("EXPECTED", String.valueOf(landmark) + ", "
                        +String.valueOf(landmark_Pos_Conf[0][landmark]) + ", "
                        + String.valueOf(landmark_Pos_Conf[1][landmark]) + ", "
                        + String.valueOf(landmark_Pos_Conf[2][landmark]) + ", "
                        + String.valueOf(landmark_Pos_Conf[3][landmark]) + ", ");

            }
            try {

                writer.write(getDateTime());
                writer.write(", ");
                for (int i = 0; i < POSE_LANDMARKS.length; i++) {
                    for (int j = 0; j < 4; j++) {
                        writer.write(Float.toString(landmark_Pos_Conf[j][i]));
                        writer.write(", ");
                    }
                }
                writer.newLine();

                Log.d("EXPECTED", "Handler has posted new writing task to writing thread");


            } catch (IOException e) {
                Log.d("EXPECTED", "Something went wrong in writing data to file");
            }
        }else{
            Log.d("EXPECTED", "Empty landmarks!");
        }


    }


    public void endRecording() {
        // Do something in response to button
        Log.d("EXPECTED", "User has ended recording");

        mThread.quitSafely();
        this.finishAffinity();


    }

}

