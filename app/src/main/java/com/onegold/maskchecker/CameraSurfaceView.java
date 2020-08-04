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
    public static final long TIME_INTERVAL = 1000;
    private long lastTime;

    private SurfaceHolder mholder;
    private Camera camera;
    private Context context;

    public CameraSurfaceView(Context context) {
        super(context);

        this.context = context;
        this.mholder = getHolder();
        this.mholder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        camera.setDisplayOrientation(90);

        try {
            camera.setPreviewDisplay(mholder);
            camera.setPreviewCallback(this);
            lastTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (camera == null)
            return;

        long currentTime = System.currentTimeMillis();
        long delay = currentTime - lastTime;

        if (delay >= TIME_INTERVAL) {
            lastTime = currentTime;

//            ((MainActivity)context).drawRect(pos);
//            if(pos>500){
//                pos -= 50;
//            }else{
//                pos += 50;
//            }
            Camera.Parameters parameters = camera.getParameters();
            InputImage image = InputImage.fromByteArray(data,
                    parameters.getPreviewSize().width,
                    parameters.getPreviewSize().height,
                    90,
                    InputImage.IMAGE_FORMAT_NV21);
            final float widthRatio = getWidth() / (float)parameters.getPreviewSize().height;
            final float heightRatio = getHeight() / (float)parameters.getPreviewSize().width;
            final float wRatioRound = (float)Math.round(widthRatio * 100) / 100;
            final float hRatioRound = (float)Math.round(heightRatio * 100) / 100;
            Log.d("!!!!", "preview : " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
            Log.d("!!!!", "view : " + getWidth() + ", " + getHeight());
            Log.d("!!!!", "ratio : " + wRatioRound + ", " + hRatioRound);
            FaceDetector detector = FaceDetection.getClient();

            Task<List<Face>> result = detector.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<List<Face>>() {
                                @Override
                                public void onSuccess(List<Face> faces) {
                                    if(context != null) {
                                        ((MainActivity) context).drawFaceRect(faces, wRatioRound, hRatioRound);
                                    }
//                                    for (Face face : faces) {
//                                        Rect bounds = face.getBoundingBox();
//                                        float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
//                                        float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
//
//                                        Log.d("############", "b" + bounds.bottom + "l" + bounds.left + "r" + bounds.right + "t" + bounds.top);
                                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                    // nose available):
//                                        FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
//                                        if (leftEar != null) {
//                                            PointF leftEarPos = leftEar.getPosition();
//                                        }

                                    // If contour detection was enabled:
//                                        List<PointF> leftEyeContour =
//                                                face.getContour(FaceContour.LEFT_EYE).getPoints();
//                                        List<PointF> upperLipBottomContour =
//                                                face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();

                                    // If classification was enabled:
//                                        if (face.getSmilingProbability() != null) {
//                                            float smileProb = face.getSmilingProbability();
//                                        }
//                                        if (face.getRightEyeOpenProbability() != null) {
//                                            float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                        }

                                    // If face tracking was enabled:
//                                        if (face.getTrackingId() != null) {
//                                            int id = face.getTrackingId();
//                                        }
//                                    }
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
    }
}
