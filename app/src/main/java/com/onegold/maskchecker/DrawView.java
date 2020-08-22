package com.onegold.maskchecker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.face.Face;

import java.util.List;

public class DrawView extends View {
    private Bitmap cacheBitmap;
    private Canvas canvas;
    private Paint paint;

    // 메인 액티비티에 영역 그리는 권한 부여
    public interface FaceDetector {
        void drawFaceRect(float left, float top, float right, float bottom, float wRatio, float hRatio);
        void drawMaskRect(float left, float top, float right, float bottom, float wRatio, float hRatio);
    }

    public DrawView(Context context) {
        super(context);
        init(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // 페인트 색상, 두께 결정
    private void init(Context context) {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }

    // 얼굴 영역 그리기
    public void drawFaceRect(float left, float top, float right, float bottom) {
        if (canvas == null && paint == null)
            return;

        paint.setColor(Color.RED);
        drawRect(left, top, right, bottom);
        paint.setColor(Color.BLUE);
    }

    // 마스크 영역 그리기
    public void drawMaskRect(float left, float top, float right, float bottom) {
        if (canvas == null)
            return;

        drawRect(left, top, right, bottom);
    }

    // 사각 영역 그리기
    public void drawRect(float left, float top, float right, float bottom) {
        // 이전 화면 clear
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        // 해당 좌표 값에 영역 그리기
        canvas.drawRect(left, top, right, bottom, paint);
        // 뷰 업데이트
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas();
        canvas.setBitmap(cacheBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cacheBitmap != null && canvas != null) {
            canvas.drawBitmap(cacheBitmap, 0, 0, null);
        }
    }
}
