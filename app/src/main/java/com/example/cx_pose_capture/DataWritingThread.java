package com.example.cx_pose_capture;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class DataWritingThread extends HandlerThread {

    private Handler myHandler;

    public DataWritingThread(String name) {
        super(name);
        myHandler = new Handler(Looper.getMainLooper());
    }


    public Handler getHandler(){
        return myHandler;
    }
}