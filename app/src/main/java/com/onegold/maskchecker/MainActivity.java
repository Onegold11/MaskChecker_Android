package com.onegold.maskchecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

public class MainActivity extends AppCompatActivity
        implements AutoPermissionsListener {
    CameraSurfaceView surfaceView;
    DrawView drawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout previewFrame = findViewById(R.id.previewFrame);
        surfaceView = new CameraSurfaceView(this);
        previewFrame.addView(surfaceView);

        drawView = findViewById(R.id.drawView);

        AutoPermissions.Companion.loadAllPermissions(this, 1);
    }

    public void drawRect(float pos){
        drawView.drawRect(pos);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDenied(int i, String[] strings) {

    }

    @Override
    public void onGranted(int i, String[] strings) {

    }
}