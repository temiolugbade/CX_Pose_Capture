package com.example.cx_pose_capture;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                boolean granted = true;
                for(String permission : REQUIRED_PERMISSIONS){
                    if(!isGranted.get(permission)){
                        granted = false;
                    }
                }
                if(granted){
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Log.d("EXPECTED", "Permissions NOW granted");
                    enableCamera();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.

                    Log.d("EXPECTED", "Permissions NOT granted");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user taps the Send button */
    public void startRecording(View view) {
        // Do something in response to button
        if (hasPermissions())
        {
            Log.d("EXPECTED", "Permission already granted");
            enableCamera();
        }
        else
        {
            Log.d("EXPECTED", "Permission request");
            requestPermissions();
        }


    }

    private boolean hasPermissions() {
        boolean granted = true;
        for (String permission : REQUIRED_PERMISSIONS) {;
            if (!(ContextCompat.checkSelfPermission(
                    this,
                    permission
            ) == PackageManager.PERMISSION_GRANTED)){
                granted = false;
            }
        }

        return granted;
    }

    private void requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS);

    }

    private void enableCamera(){

        Intent intent = new Intent(this, RecordingActivity.class);
        startActivity(intent);

    }
}