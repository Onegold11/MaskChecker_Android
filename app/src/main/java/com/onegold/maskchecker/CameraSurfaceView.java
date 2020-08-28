package com.onegold.maskchecker;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
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
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class CameraSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback, OnSuccessListener<List<Face>> {
    public interface TFLiteRequest{
        public abstract Interpreter getTFLiteInterpreter(String modelPath);
    }

    public static final int CAM_ORIENTATION = 90; // 카메라 각도
    public static final long TIME_INTERVAL = 800; // 얼굴 탐지 시간 간격
    public static final int IMAGE_SIZE = 128; // 입력 이미지 크기
    public static final int CAM_IMAGE_WIDTH = 360; // 카메라 이미지 너비
    public static final int CAM_IMAGE_HEIGHT = 480; // 카메라 이미지 높이

    private long lastTime; // 마지막 얼굴 탐지 시간
    private long currentTime; // 현재 시간
    private int cleanCount = 0;


    private SurfaceHolder mholder;
    private Camera camera;
    private TFLiteRequest context;

    private TFLiteBitmapBuilder builder;
    private DrawView drawView;
    private TFLiteModel model; // TensorFlow Lite 모델

    private float[] ratio; // 카메라와 화면 비율

    public CameraSurfaceView(Context context) {
        super(context);

        this.context = (TFLiteRequest) context;
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

        /* TFLite 모델 불러오기 */
        model = new TFLiteModel(context.getTFLiteInterpreter(TFLiteModel.MODEL_NAME));

        /* 카메라 화면과 얼굴 표시 화면의 비율을 계산 (width, height) */
        ratio = getRatioPreview(CAM_IMAGE_WIDTH, CAM_IMAGE_HEIGHT);
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
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
    }

    // 카메라 프레임 마다 호출 (1 프레임 : 대략 0.068초)
    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        // 시간 간격 측정
        currentTime = System.currentTimeMillis();
        long delay = currentTime - lastTime;

        // 지난 얼굴 탐지 후 일정 시간이 지났을 때만 실행
        if (delay < TIME_INTERVAL)
            return;
        lastTime = currentTime;

        // 기존 얼굴 영역 지우기
        if (cleanCount > 2)
            clean();
        cleanCount++;

        /* 카메라 이미지 설정 값 */
        Camera.Parameters parameters = camera.getParameters();

        /* 카메라 이미지 변환 */
        builder = new TFLiteBitmapBuilder();
        Bitmap bitmap = builder
                .getBitmapFromPreviewImage(data, parameters)
                .rotateBitmap(CAM_ORIENTATION)
                .resizeBitmap(CAM_IMAGE_WIDTH, CAM_IMAGE_HEIGHT)
                .build();

        /* Bitmap 카메라 이미지 InputImage 변환 */
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // 얼굴 탐색기
        FaceDetector detector = FaceDetection.getClient();

        // 얼굴 탐색 시작
        Task<List<Face>> result = detector.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
    }

    @Override
    public void onSuccess(List<Face> faces) {
        /* 얼굴 탐색 성공 */
        if (context != null && faces != null && faces.size() != 0) {
            /* 카메라 이미지 저장 */
            Bitmap origin = builder.build();

            /* 기존 이미지 지우기 */
            clean();
            cleanCount = 0;

            /* 탐색된 얼굴들에 대한 처리 */
            for (int i = 0; i < faces.size(); i++) {
                /* 얼굴 영역 추출 */
                Bitmap bitmap = builder
                        .setBitmap(origin)
                        .cropFaceBitmap(faces.get(i).getBoundingBox())
                        .resizeBitmap(IMAGE_SIZE, IMAGE_SIZE)
                        .build();

                /* 얼굴 영역 */
                int[] face = builder.getFace();

                /* 모델 입력 데이터 생성 */
                ByteBuffer input_img = getByteBuffer(bitmap);

                // 모델 출력 형식 지정 및 모델 실행*/
                float[][] output = model.run(input_img);

                /* 결과 DrawView에 반영 */
                drawRecognitionResult(output, face);
            }
        }
    }

    /* Bitamp -> ByteBuffer */
    private ByteBuffer getByteBuffer(Bitmap bitmap) {
        // 비트맵 픽셀 배열로 변환
        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        /* ByteBuffer 초기화 */
        ByteBuffer input = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4);
        input.order(ByteOrder.nativeOrder());

        /* ByteBuffer 입력 */
        for (int i = 0; i < IMAGE_SIZE * IMAGE_SIZE; i++) {
            int pixel = pixels[i];

            input.putFloat((float) (((pixel >> 16) & 0xff) / 127.5) - 1);
            input.putFloat((float) (((pixel >> 8) & 0xff) / 127.5) - 1);
            input.putFloat((float) ((pixel & 0xff) / 127.5) - 1);
        }

        return input;
    }

    /* 카메라 프리뷰와 DrawView의 크기 비율 반환 */
    private float[] getRatioPreview(int width, int height) {
        // width ratio, height ratio
        float[] ratio = new float[2];

        float widthRatio = getWidth() / (float) width;
        float heightRatio = getHeight() / (float) height;
        ratio[0] = (float) Math.round(widthRatio * 100) / 100;
        ratio[1] = (float) Math.round(heightRatio * 100) / 100;

        return ratio;
    }

    /* 인식 결과에 따라 DrawView에 그리기 */
    private void drawRecognitionResult(float[][] output, int[] face) {
        if(drawView != null) {
            /* 마스크 착용한 얼굴 */
            if (output[0][0] > output[0][1]) {
                /* 인식 정확도 문자열 변환 */
                String accuracy = getAccuracyToString(output[0][0]);
                drawView.drawMaskRect(accuracy, face[0] * ratio[0], face[1] * ratio[1], face[2] * ratio[0], face[3] * ratio[1]);
            }
            /* 마스크를 착용하지 않은 얼굴 */
            if (output[0][0] < output[0][1]) {
                /* 인식 정확도 문자열 변환 */
                String accuracy = getAccuracyToString(output[0][1]);
                drawView.drawFaceRect(accuracy, face[0] * ratio[0], face[1] * ratio[1], face[2] * ratio[0], face[3] * ratio[1]);
            }
        }
    }

    /* 인식 정확도 문자열 변환 */
    private String getAccuracyToString(float accuracy) {
        return String.format("%.3f", accuracy * 100);
    }

    /* DrawView clean */
    private void clean(){
        if(drawView != null)
            drawView.cleanView();
    }
    public DrawView getDrawView() {
        return drawView;
    }

    public void setDrawView(DrawView drawView) {
        this.drawView = drawView;
    }
}
