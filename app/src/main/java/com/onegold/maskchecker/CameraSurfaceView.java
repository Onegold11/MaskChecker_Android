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
import java.util.Arrays;
import java.util.List;

public class CameraSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final int CAM_ORIENTATION = 90; // 카메라 각도
    public static final long TIME_INTERVAL = 1000; // 얼굴 탐지 시간 간격
    public static final int IMAGE_SIZE = 64;
    private long lastTime; // 마지막 얼굴 탐지 시간

    private SurfaceHolder mholder;
    private Camera camera;
    private Context context;

    private Interpreter interpreter;

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

        /* TFLite 모델 불러오기 */
        interpreter = ((MainActivity) context).getTFLiteInterpreter("MobileNet.tflite");
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

    // 카메라 프레임 마다 호출 (1 프레임 : 대략 0.068초)
    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        // 시간 간격 측정
        long currentTime = System.currentTimeMillis();
        long delay = currentTime - lastTime;

        // 지난 얼굴 탐지 후 일정 시간이 지났을 때만 실행
        if (delay < TIME_INTERVAL)
            return;

        // 현재 시간 저장
        lastTime = currentTime;

        // 카메라 이미지 변환
        // 시계 방향 회전
        final Camera.Parameters parameters = camera.getParameters();
        final InputImage image = InputImage.fromByteArray(data,
                parameters.getPreviewSize().width,
                parameters.getPreviewSize().height,
                CAM_ORIENTATION,
                InputImage.IMAGE_FORMAT_YV12);

        // 얼굴 탐색기
        FaceDetector detector = FaceDetection.getClient();
        // 얼굴 탐색 시작
        Task<List<Face>> result = detector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            // 얼굴 탐색 성공
                            @Override
                            public void onSuccess(List<Face> faces) {
                                if (context != null && faces != null && faces.size() != 0) {
                                    /* 모델 입력 이미지 Builder */
                                    TFLiteBitmapBuilder builder = new TFLiteBitmapBuilder();
                                    Bitmap bitmap = builder
                                            .getBitmapFromPreviewImage(data, parameters)
                                            .rotateBitmap(CAM_ORIENTATION)
                                            .cropFaceBitmap(faces.get(0).getBoundingBox())
                                            .resizeBitmap(IMAGE_SIZE, IMAGE_SIZE)
                                            .build();

                                    /* 얼굴 영역 */
                                    List<Integer> face = builder.getFace();

                                    /* 모델 입력 데이터 생성 */
                                    float[][][][] input = getRGBArray(bitmap);

                                    // 모델 출력 형식 지정 */
                                    float[][] output = new float[1][2];

                                    /* 얼굴 사진 미리보기 출력(테스트용) */
                                    ((MainActivity) context).setImageViewImage(bitmap);

                                    /* 저장된 모델 실행 */
                                    if (interpreter != null)
                                        interpreter.run(input, output);
                                    Log.d("Result!!!", Arrays.toString(output[0]));

                                    float[] ratio = getRatioPreview(parameters.getPreviewSize().height, parameters.getPreviewSize().width);

                                    /* 결과 DrawView에 반영 */
                                    drawRecognitionResult(output, face, ratio[0], ratio[1]);

                                    bitmap.recycle();
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
    }

    /* 비트맵을 1 * 64 * 64 * 3 RGB 배열로 변환 */
    private float[][][][] getRGBArray(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float[][][][] array = new float[1][width][height][3];
        float[][][][] test = new float[1][width][height][3];

        // 비트맵 픽셀 배열로 변환
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // 픽셀 RGB 번호 저장
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int index = y * width + x;

                // RGB 0 ~ 1 변환 및 저장
                array[0][x][y][0] = (float) (((pixels[index] >> 16) & 0xff) / 255.0);
                array[0][x][y][1] = (float) (((pixels[index] >> 8) & 0xff) / 255.0);
                array[0][x][y][2] = (float) ((pixels[index] & 0xff) / 255.0);

                Log.d("testQWEQE1", "(" + x + ", " + y + ")" + array[0][x][y][0] + ", " + array[0][x][y][1] + ", " + array[0][x][y][2]);
            }
        }
        return array;
    }

    /* 카메라 프리뷰와 DrawView의 크기 비율 반환 */
    private float[] getRatioPreview(int width, int height){
        // width ratio, height ratio
        float[] ratio = new float[2];

        float widthRatio = getWidth() / (float)width;
        float heightRatio = getHeight() / (float)height;
        ratio[0] = (float) Math.round(widthRatio * 100) / 100;
        ratio[1] = (float) Math.round(heightRatio * 100) / 100;

        return ratio;
    }

    /* 인식 결과에 따라 DrawView에 그리기 */
    private void drawRecognitionResult(float[][] output, List<Integer> face, float width, float height){
        if (output[0][0] > output[0][1]) {
            ((MainActivity) context).drawMaskRect(face.get(0), face.get(1), face.get(2), face.get(3), width, height);
        } else if (output[0][0] < output[0][1]) {
            ((MainActivity) context).drawFaceRect(face.get(0), face.get(1), face.get(2), face.get(3), width, height);
        } else {
            ((MainActivity) context).cleanDrawView();
        }
    }
}
