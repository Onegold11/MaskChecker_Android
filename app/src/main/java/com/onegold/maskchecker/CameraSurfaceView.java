package com.onegold.maskchecker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class CameraSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final int CAM_ORIENTATION = 90; // 카메라 각도
    public static final long TIME_INTERVAL = 1000; // 얼굴 탐지 시간 간격
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
    public void onPreviewFrame(final byte[] data, Camera camera) {
        try {
            // 시간 간격 측정
            long currentTime = System.currentTimeMillis();
            long delay = currentTime - lastTime;

            // 지난 얼굴 탐지 후 일정 시간이 지났을 때
            if (delay >= TIME_INTERVAL) {
                lastTime = currentTime;

                // 카메라 이미지 변환
                final Camera.Parameters parameters = camera.getParameters();
                final InputImage image = InputImage.fromByteArray(data,
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
                                        if (context != null && faces != null && faces.size() != 0){
                                            Rect bounds = faces.get(0).getBoundingBox();
                                            float left = bounds.left * wRatioRound;
                                            float top = bounds.top * hRatioRound;
                                            float right = bounds.right * wRatioRound;
                                            float bottom = bounds.bottom * hRatioRound;

                                            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);

                                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                                            yuv.compressToJpeg(new Rect(0, 0, parameters.getPreviewSize().width, parameters.getPreviewSize().height), 50, out);

                                            byte[] bytes = out.toByteArray();
                                            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                            Matrix matrix = new Matrix();
                                            matrix.postRotate(CAM_ORIENTATION);
                                            Bitmap rotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                                            Log.d("QQQQQQ", bounds.left + "");
                                            Log.d("QQQQQQ", bounds.top + "");
                                            Log.d("QQQQQQ", bounds.right + "");
                                            Log.d("QQQQQQ", bounds.bottom + "");
                                            Bitmap result = Bitmap.createBitmap(rotate,
                                                    bounds.left,
                                                    bounds.top,
                                                    bounds.right - bounds.left,
                                                    bounds.bottom - bounds.top);
                                            ((MainActivity)context).setImageViewImage(result);
                                        }
                                        /*
                                        ((MainActivity)context).setImageViewImage(image.getBitmapInternal());
                                        // 얼굴 영역 표시
                                        if (context != null && faces != null) {
                                            for (Face face : faces) {
                                                // 미리 보기 화면에서의 좌표 값 계산
                                                Rect bounds = face.getBoundingBox();
                                                float left = bounds.left * wRatioRound;
                                                float top = bounds.top * hRatioRound;
                                                float right = bounds.right * wRatioRound;
                                                float bottom = bounds.bottom * hRatioRound;

                                                // 학습 모델로 검증
                                                // 함수
                                                // if 마스크 일 경우
                                                ((MainActivity) context).drawFaceRect(left, top, right, bottom);
                                            }
                                        }*/
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
        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }
}
