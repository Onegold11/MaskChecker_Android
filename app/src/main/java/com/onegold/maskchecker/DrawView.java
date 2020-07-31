package com.onegold.maskchecker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawView extends View {
    private Bitmap cacheBitmap;
    private Canvas canvas;
    private Paint paint;

    public DrawView(Context context) {
        super(context);
        init(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void drawRect(float pos){
        if(cacheBitmap != null){
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawRect(pos, pos, pos + 100, pos + 100, paint);
            Log.d("!!!!!!!!!!!", "pos" + pos);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas();
        canvas.setBitmap(cacheBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(cacheBitmap != null){
            canvas.drawBitmap(cacheBitmap, 0, 0, null);
        }
    }
}
