package com.example.cx_pose_capture;


import com.google.mlkit.vision.pose.PoseLandmark;


import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Util {

    public final static int[] MLkit_POSE_LANDMARKS = {PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE, PoseLandmark.LEFT_PINKY, PoseLandmark.RIGHT_PINKY,
            PoseLandmark.LEFT_INDEX, PoseLandmark.RIGHT_INDEX, PoseLandmark.LEFT_THUMB, PoseLandmark.RIGHT_THUMB,
            PoseLandmark.LEFT_HEEL, PoseLandmark.RIGHT_HEEL, PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.RIGHT_FOOT_INDEX,
            PoseLandmark.NOSE, PoseLandmark.LEFT_EYE_INNER, PoseLandmark.LEFT_EYE, PoseLandmark.LEFT_EYE_OUTER,
            PoseLandmark.RIGHT_EYE_INNER, PoseLandmark.RIGHT_EYE, PoseLandmark.RIGHT_EYE_OUTER,
            PoseLandmark.LEFT_EAR, PoseLandmark.RIGHT_EAR, PoseLandmark.LEFT_MOUTH, PoseLandmark.RIGHT_MOUTH};


    public static String getDateTime(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_SSS");
        return simpleDateFormat.format(calendar.getTime()).toString();
    }



}
