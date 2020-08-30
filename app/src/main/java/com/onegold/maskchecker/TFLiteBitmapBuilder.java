package com.onegold.maskchecker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;

public class TFLiteBitmapBuilder {
    private Bitmap bitmap;
    private int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int[] face;

    public TFLiteBitmapBuilder() {
        face = new int[4];
    }

    public TFLiteBitmapBuilder setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        return this;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int[] getFace() {
        return face;
    }

    /* 사진 프리뷰 데이터 Bitmap 객체로 변환 */
    public TFLiteBitmapBuilder getBitmapFromPreviewImage(final byte[] data, final Camera.Parameters parameters) {
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, parameters.getPreviewSize().width, parameters.getPreviewSize().height), 50, out);

        byte[] bytes = out.toByteArray();

        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return this;
    }

    /* 카메라 방향 설정 */
    public TFLiteBitmapBuilder setCameraFacing(int id){
        cameraID = id;
        /* 전면 카메라일 경우 좌우 반전 */
        if(cameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            Matrix matrix = new Matrix();
            matrix.setScale(-1, 1);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        }
        return this;
    }

    /* 사진 프리뷰 회전 */
    public TFLiteBitmapBuilder rotateBitmap(int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return this;
    }

    /* 사진 영역 추출 */
    public TFLiteBitmapBuilder cropFaceBitmap(Rect bounds) {
        if(bitmap != null) {
            getFaceRectPos(bounds, bitmap.getWidth(), bitmap.getHeight());
            bitmap = Bitmap.createBitmap(bitmap, face[0], face[1],
                    face[2] - face[0], face[3] - face[1]);
        }
        return this;
    }

    /* 비트맵 크기 resize */
    public TFLiteBitmapBuilder resizeBitmap(int width, int height) {
        if(bitmap != null) {
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        return this;
    }

    /* 얼굴 영역 좌표 */
    private void getFaceRectPos(Rect bounds, int width, int height) {
        int start_x = bounds.left - 10;
        int start_y = bounds.top - 20;
        int end_x = bounds.right + 10;
        int end_y = bounds.bottom + 20;

        if (start_x < 0)
            start_x = 0;
        if (start_y < 0)
            start_y = 0;
        if (end_x > width)
            end_x = width;
        if (end_y > height)
            end_y = height;

        face[0] = start_x;
        face[1] = start_y;
        face[2] = end_x;
        face[3] = end_y;
    }

    public Bitmap build() {
        return bitmap;
    }
}
