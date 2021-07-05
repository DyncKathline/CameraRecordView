package com.kathline.cameralib.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.kathline.cameralib.R;

public class TypeButton extends View {
    public static final int TYPE_CANCEL = 0x001;
    public static final int TYPE_CONFIRM = 0x002;
    private int button_type;
    private int button_size;

    private float center_X;
    private float center_Y;
    private float button_radius;

    private Paint mPaint;
    private Path path;
    private float strokeWidth;

    private float index;
    private RectF rectF;
    private int accentColor;

    public TypeButton(Context context) {
        super(context);
        init();
    }

    public TypeButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TypeButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
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
        button_size = (int) (layout_width / 5f);
        layout_height = button_size + (button_size / 5) * 2 + 100;

        config(getContext(), TYPE_CANCEL, button_size);
    }

    public void setType(int type) {
        this.button_type = type;
        invalidate();
    }

    public void setSize(int size) {
        button_size = size;
        button_radius = size / 2.0f;
        center_X = size / 2.0f;
        center_Y = size / 2.0f;

        mPaint = new Paint();
        path = new Path();
        strokeWidth = size / 50f;
        index = button_size / 12f;
        rectF = new RectF(center_X, center_Y - index, center_X + index * 2, center_Y + index);

    }

    public TypeButton(Context context, int type, int size) {
        super(context);
        config(context, type, size);
    }

    private void config(Context context, int type, int size) {
        this.button_type = type;

        setSize(size);
        accentColor = ContextCompat.getColor(context, R.color.photos_camera_fg_accent);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(button_size, button_size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //如果类型为取消，则绘制内部为返回箭头
        if (button_type == TYPE_CANCEL) {
            mPaint.setAntiAlias(true);
            mPaint.setColor(0xEEDCDCDC);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(center_X, center_Y, button_radius, mPaint);

            mPaint.setColor(Color.BLACK);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);

            path.moveTo(center_X - index / 7, center_Y + index);
            path.lineTo(center_X + index, center_Y + index);

            path.arcTo(rectF, 90, -180);
            path.lineTo(center_X - index, center_Y - index);
            canvas.drawPath(path, mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            path.reset();
            path.moveTo(center_X - index, (float) (center_Y - index * 1.5));
            path.lineTo(center_X - index, (float) (center_Y - index / 2.3));
            path.lineTo((float) (center_X - index * 1.6), center_Y - index);
            path.close();
            canvas.drawPath(path, mPaint);

        }
        //如果类型为确认，则绘制绿色勾
        if (button_type == TYPE_CONFIRM) {
            mPaint.setAntiAlias(true);
            mPaint.setColor(0xFFFFFFFF);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(center_X, center_Y, button_radius, mPaint);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(accentColor);
            mPaint.setStrokeWidth(strokeWidth);

            path.moveTo(center_X - button_size / 6f, center_Y);
            path.lineTo(center_X - button_size / 21.2f, center_Y + button_size / 7.7f);
            path.lineTo(center_X + button_size / 4.0f, center_Y - button_size / 8.5f);
            path.lineTo(center_X - button_size / 21.2f, center_Y + button_size / 9.4f);
            path.close();
            canvas.drawPath(path, mPaint);
        }
    }
}
