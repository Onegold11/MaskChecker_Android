package com.onegold.maskchecker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class CameraSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final int CAM_ORIENTATION = 90; // 카메라 각도
    public static final long TIME_INTERVAL = 500; // 얼굴 탐지 시간 간격
    private long lastTime; // 마지막 얼굴 탐지 시간

    private SurfaceHolder mholder;
    private Camera camera;
    private Context context;

    public CameraSurfaceView(Context context) {
        super(context);

        this.context = context;
        this.mholder = getHolder();
        this.mholder.addCallback(this);
    }

    // 미리 보기 화면 생성
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        // 카메라 Auto focus mode on
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        camera.setParameters(parameters);

        // 카메라 90도 회전, 세로 방향으로만 작동
        camera.setDisplayOrientation(CAM_ORIENTATION);

        try {
            camera.setPreviewDisplay(mholder);
            camera.setPreviewCallback(this);
            lastTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 미리 보기 화면 변화
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.startPreview();
    }

    // 미리 보기 화면 종료
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    // 카메라 프레임 마다 호출 (대략 0.068초)
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            // 시간 간격 측정
            long currentTime = System.currentTimeMillis();
            long delay = currentTime - lastTime;

            // 지난 얼굴 탐지 후 일정 시간이 지났을 때
            if (delay >= TIME_INTERVAL) {
                lastTime = currentTime;

                // 카메라 이미지 변환
                Camera.Parameters parameters = camera.getParameters();
                InputImage image = InputImage.fromByteArray(data,
                        parameters.getPreviewSize().width,
                        parameters.getPreviewSize().height,
                        CAM_ORIENTATION,
                        InputImage.IMAGE_FORMAT_NV21);

                // 카메라 이미지와 미리 보기 화면의 해상도 차이로 인한 좌표 변환 비율 계산
                final float widthRatio = getWidth() / (float) parameters.getPreviewSize().height;
                final float heightRatio = getHeight() / (float) parameters.getPreviewSize().width;
                final float wRatioRound = (float) Math.round(widthRatio * 100) / 100;
                final float hRatioRound = (float) Math.round(heightRatio * 100) / 100;

                // 얼굴 탐색기
                FaceDetector detector = FaceDetection.getClient();

                // 얼굴 탐색 시작
                Task<List<Face>> result = detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    // 얼굴 탐색 성공
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // 얼굴 영역 표시
                                        if (context != null) {
                                            ((MainActivity) context).drawFaceRect(faces, wRatioRound, hRatioRound);
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    // 얼굴 탐색 실패
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                    }
                                });

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
