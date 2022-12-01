package com.example.cx_pose_capture;


import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

public class DataWritingThread extends HandlerThread {

    private Handler myHandler;
    private BufferedWriter myWriter;
    private String storageDirectoryName = "TCC_POSE";

    public DataWritingThread(String name) {
        super(name);
        myHandler = new Handler(Looper.myLooper());
        //createWriteFolder();
    }



    public Handler extraSetup(BufferedWriter writer){

        myWriter = writer;
        return myHandler;
    }


    public static BufferedWriter createWriteFolder(Context c){

        String storageDirectoryName = "EnTimeMent_Pose";
        BufferedWriter myWriter;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            //fail safely
            Log.d("EXPECTED", "I CANNOT write to storage");

            return null;
        }else{

            //File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    //storageDirectoryName);
            File dir = new File(c.getExternalFilesDir(null), storageDirectoryName);

            if (!dir.mkdirs()) {
                if(!dir.exists()) {
                    Log.d("EXPECTED", "Could NOT create directory");
                }
            }

            File file = new File(dir, storageDirectoryName+" "+Util.getDateTime()+".txt");
            try {
                myWriter = new BufferedWriter(new FileWriter(file));
            }catch(IOException e){
                myWriter = null;
                Log.d("EXPECTED", "Writer NOT created");


            }

            return myWriter;

        }



    }

    public void writePoseToFile(Pose pose){

        // Get all PoseLandmarks. If no person was detected, the list will be empty
        List<PoseLandmark> allPoseLandmarks = pose.getAllPoseLandmarks();

        if (!allPoseLandmarks.isEmpty()){



            // Or get specific PoseLandmarks individually. These will all be null if no person
            // was detected
            float[][] landmark_Pos_Conf = new float[4][Util.MLkit_POSE_LANDMARKS.length];
            PointF3D landmark_Pos3D;
            for (int landmark : Util.MLkit_POSE_LANDMARKS) {
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

                myWriter.write(Util.getDateTime());
                myWriter.write(", ");
                for (int i = 0; i < Util.MLkit_POSE_LANDMARKS.length; i++) {
                    for (int j = 0; j < 4; j++) {
                        myWriter.write(Float.toString(landmark_Pos_Conf[j][i]));
                        myWriter.write(", ");
                    }
                }
                myWriter.newLine();

                Log.d("EXPECTED", "Handler has posted new writing task to writing thread");


            } catch (IOException e) {
                Log.d("EXPECTED", "Something went wrong in writing data to file");
            }
        }else{
            Log.d("EXPECTED", "Empty landmarks!");
        }


    }

}