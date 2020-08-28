package com.onegold.maskchecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.mlkit.vision.face.Face;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements AutoPermissionsListener, CameraSurfaceView.TFLiteRequest{
    private CameraSurfaceView surfaceView;
    private DrawView drawView;
    private FrameLayout previewFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 권한 설정
        AutoPermissions.Companion.loadAllPermissions(this, 101);

        /* 화면 안 꺼지게 설정 */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 얼굴 영역 그리는 뷰
        drawView = findViewById(R.id.drawView);

        // 카메라 미리 보기 뷰
        previewFrame = findViewById(R.id.previewFrame);
        surfaceView = new CameraSurfaceView(this);
        surfaceView.setDrawView(drawView);
        previewFrame.addView(surfaceView);
    }

    /*  TFLite 모델 생성 및 반환 */
    @Override
    public Interpreter getTFLiteInterpreter(String modelPath){
        try{
            return new Interpreter(loadModelFile(MainActivity.this, modelPath));
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /* TFLite 모델 파일 불러오기 */
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // 밑으로는 권한 설정 관련 메서드
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int i, String[] strings) {
        for(String permission : strings){
            if(permission.equals("android.permission.CAMERA")){
                finish();
            }
        }
    }

    @Override
    public void onGranted(int i, String[] strings) {

    }
}