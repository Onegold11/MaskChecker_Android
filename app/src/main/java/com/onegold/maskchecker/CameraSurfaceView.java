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
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceLandmark;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.QuantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
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
                                            /* 얼굴 영역 */
                                            Rect bounds = faces.get(0).getBoundingBox();

                                            /* 사진 프리뷰 데이터 Bitmap 객체로 변환 */
                                            Bitmap bitmap = getBitmapFromPreviewImage(data, parameters);

                                            /* 사진 프리뷰 회전(90) */
                                            Bitmap rotate = rotateBitmap(bitmap, CAM_ORIENTATION);

                                            /* 얼굴 영역(왼쪽, 위, 오른쪽, 아래) */
                                            List<Integer> face = getFaceRectPos(bounds, rotate.getWidth(), rotate.getHeight());

                                            /* 사진 얼굴 영역 추출 */
                                            Bitmap result = Bitmap.createBitmap(rotate,
                                                    face.get(0),
                                                    face.get(1),
                                                    face.get(2) - face.get(0),
                                                    face.get(3) - face.get(1));
                                            rotate.recycle();

                                            /* 모델 입력 형식으로 변환 */
                                            TensorImage tImage = new TensorImage(DataType.UINT8);
                                            ImageProcessor imageProcessor =
                                                    new ImageProcessor.Builder()
                                                            .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                                                            .add(new QuantizeOp(0, (float)(1/255.0)))
                                                            .build();
                                            tImage.load(result);
                                            tImage = imageProcessor.process(tImage);
                                            result.recycle();

                                            // 모델 출력 버퍼
                                            float[][] output = new float[1][2];
                                            /* 얼굴 사진 미리보기 출력(테스트용) */
                                            ((MainActivity) context).setImageViewImage(tImage.getBitmap());

                                            /* 저장된 모델 실행 */
                                            if(interpreter != null)
                                                interpreter.run(tImage.getBuffer(), output);
                                            Log.d("Result!!!", Arrays.toString(output[0]));
                                            Log.d("Result----------", "===========");


                                            final float widthRatio = getWidth() / (float) parameters.getPreviewSize().height;
                                            final float heightRatio = getHeight() / (float) parameters.getPreviewSize().width;
                                            final float wRatioRound = (float) Math.round(widthRatio * 100) / 100;
                                            final float hRatioRound = (float) Math.round(heightRatio * 100) / 100;

                                            if(output[0][0] > output[0][1]){
                                                ((MainActivity) context).drawMaskRect(face.get(0), face.get(1), face.get(2), face.get(3), wRatioRound, hRatioRound);
                                            }else{
                                                ((MainActivity) context).drawFaceRect(face.get(0), face.get(1), face.get(2), face.get(3), wRatioRound, hRatioRound);
                                            }
                                        }
                                        /*
                                        // 얼굴 영역 표시
                                        if (context != null && faces != null) {
                                            for (Face face : faces) {
                                                // 미리 보기 화면에서의 좌표 값 계산
                                                Rect bounds = face.getBoundingBox();
                                                float left = bounds.left * wRatioRound;
                                                float top = bounds.top * hRatioRound;
                                                float right = bounds.right * wRatioRound;
                                                float bottom = bounds.bottom * hRatioRound;

                                                // 카메라 이미지와 미리 보기 화면의 해상도 차이로 인한 좌표 변환 비율 계산
                                                final float widthRatio = getWidth() / (float) parameters.getPreviewSize().height;
                                                final float heightRatio = getHeight() / (float) parameters.getPreviewSize().width;
                                                final float wRatioRound = (float) Math.round(widthRatio * 100) / 100;
                                                final float hRatioRound = (float) Math.round(heightRatio * 100) / 100;
                                                // 학습 모델로 검증
                                                // 함수
                                                // if 마스크 일 경우
                                                ((MainActivity) context).drawFaceRect(left, top, right, bottom, wRatioRound, hRatioRound);
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

    /* 사진 프리뷰 데이터 Bitmap 객체로 변환 */
    private Bitmap getBitmapFromPreviewImage(final byte[] data, final Camera.Parameters parameters){
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, parameters.getPreviewSize().width, parameters.getPreviewSize().height), 50, out);

        byte[] bytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /* 사진 프리뷰 회전(90) */
    private Bitmap rotateBitmap(final Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();

        return rotate;
    }

    /* 얼굴 영역 좌표 */
    private List<Integer> getFaceRectPos(Rect bounds, int width, int height){
        List<Integer> list = new ArrayList<>();

        int start_x = bounds.left;
        int start_y = bounds.top;
        int end_x = bounds.right;
        int end_y = bounds.bottom;

        if (start_x < 0)
            start_x = 0;
        if (start_y < 0)
            start_y = 0;
        if (end_x > width)
            end_x = width;
        if (end_y > height)
            end_y = height;

        list.add(start_x);
        list.add(start_y);
        list.add(end_x);
        list.add(end_y);

        return list;
    }

}
