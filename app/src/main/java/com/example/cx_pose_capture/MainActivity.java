package com.example.cx_pose_capture;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;


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
        Button buttonView = findViewById(R.id.btn_StartRecording);
        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(getString(R.string.dialog_StartRecording_Title))
                        .setMessage(getString(R.string.dialog_StartRecording_Msg))
                        .setNeutralButton(getString(R.string.dialog_StartRecording_Cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing
                            }
                        })
                        .setPositiveButton(getString(R.string.dialog_StartRecording_Ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startRecording();
                            }
                        }).show();



            }
        });

    }


    public void startRecording() {
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