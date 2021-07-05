package com.kathline.cameralib.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class ReturnButton extends View {

    private int size;

    private int center_X;
    private int center_Y;
    private float strokeWidth;

    private Paint paint;
    Path path;

    public ReturnButton(Context context, int size) {
        this(context);
        config(size);
    }

    private void config(int size) {
        this.size = size;
        center_X = size / 2;
        center_Y = size / 2;

        strokeWidth = size / 15f;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);

        path = new Path();
    }

    private void init() {
        WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);

        int layout_width = 0;
        int layout_height = 0;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layout_width = outMetrics.widthPixels;
        } else {
            layout_width = outMetrics.widthPixels / 2;
        }
        int button_size = (int) (layout_width / 5f);
        config((int) (button_size / 2.5f));
    }

    public ReturnButton(Context context) {
        super(context);
        init();
    }

    public ReturnButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReturnButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(size, size / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        path.moveTo(strokeWidth, strokeWidth/2);
        path.lineTo(center_X, center_Y - strokeWidth/2);
        path.lineTo(size - strokeWidth, strokeWidth/2);
        canvas.drawPath(path, paint);
    }
}
