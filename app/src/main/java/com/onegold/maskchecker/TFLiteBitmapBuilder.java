package com.onegold.maskchecker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TFLiteBitmapBuilder {
    private Bitmap bitmap;
    private List<Integer> face;

    public TFLiteBitmapBuilder() {
        face = new ArrayList<>();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public List<Integer> getFace() {
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

    /* 사진 프리뷰 회전(90) */
    public TFLiteBitmapBuilder rotateBitmap(int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return this;
    }

    /* 사진 영역 추출 */
    public TFLiteBitmapBuilder cropFaceBitmap(Rect bounds){
        getFaceRectPos(bounds, bitmap.getWidth(), bitmap.getHeight());
        bitmap = Bitmap.createBitmap(bitmap, face.get(0), face.get(1),
                face.get(2) - face.get(0), face.get(3) - face.get(1));
        return this;
    }

    /* 비트맵 크기 resize */
    public TFLiteBitmapBuilder resizeBitmap(int width, int height){
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
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

        face.add(start_x);
        face.add(start_y);
        face.add(end_x);
        face.add(end_y);
    }

    public Bitmap build(){
        return bitmap;
    }
}
