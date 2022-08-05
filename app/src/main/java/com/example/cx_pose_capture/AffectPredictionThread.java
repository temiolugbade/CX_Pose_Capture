package com.example.cx_pose_capture;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class AffectPredictionThread extends HandlerThread {

    private Handler myHandler;
    private Random rand = new Random();

    public AffectPredictionThread(String name) {
        super(name);
        myHandler = new Handler(Looper.myLooper());
    }


    public Handler getHandler(){
        return myHandler;
    }


    public void predictAffect(Pose pose, TextView txtAffectPrediction){
        txtAffectPrediction.setText(R.string.txt_PoseClassification);
        String prediction = "";

        if (pose.getAllPoseLandmarks().isEmpty()){

            prediction = "Can't see you!";
            txtAffectPrediction.setText(prediction);
            return;

        }

        // Predict affect based on pose data


        //faux prediction
        int maxRandomInt = 6000;
        int randomInt = rand.nextInt(maxRandomInt);

        if (randomInt<=maxRandomInt/2){
            int randomIntInner = rand.nextInt(maxRandomInt);
            if (randomIntInner<=maxRandomInt/2) {
                prediction = "Feeling embarrassed";
            }
            else {
                prediction = "Feeling dissatisfied";
            }
        }
        else {
            int randomIntInner = rand.nextInt(maxRandomInt);
            if (randomIntInner<=maxRandomInt/2) {
                if (randomIntInner % 10 == 0){
                    prediction = "Can't tell how you feel";
                }
                else {
                    prediction = "Feeling good";
                }
            }
            else {
                if (randomIntInner % 10 == 0) {
                    prediction = "Feeling sexy";
                } else {
                    prediction = "Feeling playful";
                }


            }
        }

        txtAffectPrediction.setText(prediction);

    }


}