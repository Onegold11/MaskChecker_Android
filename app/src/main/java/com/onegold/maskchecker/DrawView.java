package com.onegold.maskchecker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
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
    private Paint pan;

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
        pan = new Paint();

        paint.setColor(Color.BLUE);
        pan.setColor(Color.BLUE);

        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        pan.setTextSize(72);
    }

    // 얼굴 영역 그리기
    public void drawFaceRect(String accuracy, float left, float top, float right, float bottom) {
        if (canvas == null && paint == null)
            return;

        paint.setColor(Color.RED);
        pan.setColor(Color.RED);

        drawTextAndRect(false, accuracy, left, top, right, bottom);

        paint.setColor(Color.BLUE);
        pan.setColor(Color.BLUE);
    }

    // 마스크 영역 그리기
    public void drawMaskRect(String accuracy, float left, float top, float right, float bottom) {
        if (canvas == null && paint == null)
            return;

        drawTextAndRect(true, accuracy, left, top, right, bottom);
    }

    // 텍스트와 사각 영역 그리기
    private void drawTextAndRect(boolean isMask, String accuracy, float left, float top, float right, float bottom) {
        drawRect(left, top, right, bottom);
        drawAccuracy(isMask, accuracy, left, top, right, bottom);
    }

    // 정확도 표시
    private void drawAccuracy(boolean isMask, String accuracy, float left, float top, float right, float bottom) {
        if (isMask){
            canvas.drawText("Mask", left + 20, bottom - 20, pan);
            canvas.drawText(accuracy + "%", left + 20, top + 60, pan);
        }
        else {
            canvas.drawText("No Mask", left + 20, bottom - 20, pan);
            canvas.drawText(accuracy + "%", left + 20, top + 60, pan);
        }
    }

    // 사각 영역 그리기
    private void drawRect(float left, float top, float right, float bottom) {
        // 해당 좌표 값에 영역 그리기
        canvas.drawRect(left, top, right, bottom, paint);
        // 뷰 업데이트
        invalidate();
    }

    /* 뷰에 생성된 그림 지우기 */
    public void cleanView() {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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
