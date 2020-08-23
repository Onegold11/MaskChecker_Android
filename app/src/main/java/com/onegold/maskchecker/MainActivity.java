package com.onegold.maskchecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
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

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
        imageView.invalidate();
    }

    /*  TFLite 모델 생성 및 반환 */
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

    @Override
    public void drawFaceRect(float left, float top, float right, float bottom, float wRatio, float hRatio) {
        if(drawView != null){
            drawView.drawFaceRect(left * wRatio, top * hRatio, right * wRatio, bottom * hRatio);
        }
    }

    @Override
    public void drawMaskRect(float left, float top, float right, float bottom, float wRatio, float hRatio) {
        if(drawView != null){
            drawView.drawMaskRect(left * wRatio, top * hRatio, right * wRatio, bottom * hRatio);
        }
    }

    @Override
    public void cleanDrawView() {
        if(drawView != null){
            drawView.cleanView();
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