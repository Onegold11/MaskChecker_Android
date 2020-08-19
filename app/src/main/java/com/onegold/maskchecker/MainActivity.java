package com.onegold.maskchecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.mlkit.vision.face.Face;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AutoPermissionsListener, DrawView.FaceDetector {
    CameraSurfaceView surfaceView;
    DrawView drawView;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 얼굴 영역 그리는 뷰
        drawView = findViewById(R.id.drawView);

        // 카메라 미리 보기 뷰
        FrameLayout previewFrame = findViewById(R.id.previewFrame);
        surfaceView = new CameraSurfaceView(this);
        previewFrame.addView(surfaceView);

        // 이미지 뷰
        imageView = findViewById(R.id.imageView);
        // 권한 설정
        AutoPermissions.Companion.loadAllPermissions(this, 1);
    }

    public void setImageViewImage(Bitmap bitmap){
        imageView.setImageBitmap(bitmap);
        Log.d("TEST!!!@@@", ">>>>");
        imageView.invalidate();
    }
    @Override
    public void drawFaceRect(float left, float top, float right, float bottom) {
        if(drawView != null){
            drawView.drawFaceRect(left, top, right, bottom);
        }
    }

    @Override
    public void drawMaskRect(float left, float top, float right, float bottom) {
        if(drawView != null){
            drawView.drawMaskRect(left, top, right, bottom);
        }
    }

    // 밑으로는 권한 설정 관련 메서드
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