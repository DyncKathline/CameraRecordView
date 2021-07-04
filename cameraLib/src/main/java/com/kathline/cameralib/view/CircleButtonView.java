package com.kathline.cameralib.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.kathline.cameralib.R;

public class CircleButtonView extends View {
    private static final int WHAT_LONG_CLICK = 1;
    private Paint mBigCirclePaint;
    private Paint mSmallCirclePaint;
    private Paint mProgressCirclePaint;
    private int mHeight;//当前View的高
    private int mWidth;//当前View的宽
    private float mInitBitRadius;
    private float mInitSmallRadius;
    private float mBigRadius;
    private float mSmallRadius;
    private long mStartTime;
    private long mEndTime;
    private Context mContext;
    private boolean isRecording;//录制状态
    private boolean isMaxTime;//达到最大录制时间
    private float mCurrentProgress;//当前进度

    private long mLongClickTime = 500;//长按最短时间(毫秒)，
    private long mMaxTime = 30000;//录制最大时间s
    private long mMinTime = 1000;//录制最短时间
    private int mProgressColor;//进度条颜色
    private float mProgressW = 18f;//圆环宽度

    private boolean isPressed;//当前手指处于按压状态
    private ValueAnimator mProgressAni;//圆弧进度变化

    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;      //只能拍照
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;     //只能录像
    public static final int BUTTON_STATE_BOTH = 0x103;              //两者都可以

    private int state;//按钮可执行的功能状态（拍照,录制,两者）


    public CircleButtonView(Context context) {
        super(context);
        init(context, null);
    }

    public CircleButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CircleButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleButtonView);
        mMinTime = a.getInt(R.styleable.CircleButtonView_minTime, 0);
        mMaxTime = a.getInt(R.styleable.CircleButtonView_maxTime, 10000);
        mProgressW = a.getDimension(R.styleable.CircleButtonView_progressWidth, 12f);
        mProgressColor = a.getColor(R.styleable.CircleButtonView_progressColor, Color.parseColor("#6ABF66"));
        a.recycle();
        //初始画笔抗锯齿、颜色
        mBigCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBigCirclePaint.setColor(Color.parseColor("#DDDDDD"));

        mSmallCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSmallCirclePaint.setColor(Color.parseColor("#FFFFFF"));

        mProgressCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressCirclePaint.setColor(mProgressColor);

        mProgressAni = ValueAnimator.ofFloat(0, 360f);
        mProgressAni.setDuration(mMaxTime);

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
        mWidth = (int) (layout_width / 5f);

        state = BUTTON_STATE_BOTH;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mInitBitRadius = mBigRadius = mWidth / 2 * 0.75f;
        mInitSmallRadius = mSmallRadius = mBigRadius * 0.75f;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        //绘制外圆
        canvas.drawCircle(mWidth / 2, mHeight / 2, mBigRadius, mBigCirclePaint);
        //绘制内圆
        canvas.drawCircle(mWidth / 2, mHeight / 2, mSmallRadius, mSmallCirclePaint);
        //录制的过程中绘制进度条
        if (isRecording) {
            drawProgress(canvas);
        }
    }

    /**
     * 绘制圆形进度
     *
     * @param canvas
     */
    private void drawProgress(Canvas canvas) {
        mProgressCirclePaint.setStrokeWidth(mProgressW);
        mProgressCirclePaint.setStyle(Paint.Style.STROKE);
        //用于定义的圆弧的形状和大小的界限
        RectF oval = new RectF(mWidth / 2 - (mBigRadius - mProgressW / 2), mHeight / 2 - (mBigRadius - mProgressW / 2), mWidth / 2 + (mBigRadius - mProgressW / 2), mHeight / 2 + (mBigRadius - mProgressW / 2));
        //根据进度画圆弧
        canvas.drawArc(oval, -90, mCurrentProgress, false, mProgressCirclePaint);
    }

    private Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_LONG_CLICK:
                    //长按事件触发
                    if(state == BUTTON_STATE_BOTH || state == BUTTON_STATE_ONLY_RECORDER) {
                        if (onLongClickListener != null) {
                            onLongClickListener.onLongClick();
                        }
                        //内外圆动画，内圆缩小，外圆放大
                        startAnimation(mBigRadius, mBigRadius * 1.33f, mSmallRadius, mSmallRadius * 0.7f);
                    }
                    break;
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                mStartTime = System.currentTimeMillis();
                Message mMessage = Message.obtain();
                mMessage.what = WHAT_LONG_CLICK;
                mHandler.sendMessageDelayed(mMessage, mLongClickTime);
                break;
            case MotionEvent.ACTION_UP:
                isPressed = false;
                isRecording = false;
                mEndTime = System.currentTimeMillis();
                if (mEndTime - mStartTime < mLongClickTime) {
                    mHandler.removeMessages(WHAT_LONG_CLICK);
                    if(state == BUTTON_STATE_BOTH || state == BUTTON_STATE_ONLY_CAPTURE) {
                        if (onClickListener != null)
                            onClickListener.onClick();
                    }
                } else {
                    startAnimation(mBigRadius, mInitBitRadius, mSmallRadius, mInitSmallRadius);//手指离开时动画复原
                    if (mProgressAni != null && mProgressAni.getCurrentPlayTime() < mMinTime && !isMaxTime) {
                        if (onLongClickListener != null) {
                            onLongClickListener.onNoMinRecord(mMinTime);
                        }
                        mProgressAni.cancel();
                    } else {
                        //录制完成
                        if (onLongClickListener != null && !isMaxTime) {
                            onLongClickListener.onRecordFinishedListener();
                        }
                    }
                }
                break;
        }
        return true;

    }

    private void startAnimation(float bigStart, float bigEnd, float smallStart, float smallEnd) {
        ValueAnimator bigObjAni = ValueAnimator.ofFloat(bigStart, bigEnd);
        bigObjAni.setDuration(150);
        bigObjAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBigRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        ValueAnimator smallObjAni = ValueAnimator.ofFloat(smallStart, smallEnd);
        smallObjAni.setDuration(150);
        smallObjAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mSmallRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        bigObjAni.start();
        smallObjAni.start();

        smallObjAni.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isRecording = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //开始绘制圆形进度
                if (isPressed) {
                    isRecording = true;
                    isMaxTime = false;
                    startProgressAnimation();
                }
            }


            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

    }

    /**
     * 圆形进度变化动画
     */
    private void startProgressAnimation() {
        mProgressAni.start();
        mProgressAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        mProgressAni.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //录制动画结束时，即为录制全部完成
                if (onLongClickListener != null && isPressed) {
                    isPressed = false;
                    isMaxTime = true;
                    onLongClickListener.onRecordFinishedListener();
                    startAnimation(mBigRadius, mInitBitRadius, mSmallRadius, mInitSmallRadius);
                    //影藏进度进度条
                    mCurrentProgress = 0;
                    invalidate();
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void setMaxTime(long maxTime) {
        this.mMaxTime = maxTime;
        mProgressAni.setDuration(mMaxTime);
    }

    public void setMinTime(long minTime) {
        this.mMinTime = minTime;
    }

    //设置按钮功能（拍照和录像）
    public void setFeatures(int state) {
        this.state = state;
    }

    public long getCurrentPlayTime() {
        return mProgressAni.getCurrentPlayTime() / 1000;
    }

    /**
     * 长按监听器
     */
    public interface OnLongClickListener {
        void onLongClick();

        //未达到最小录制时间
        void onNoMinRecord(long currentTime);

        //录制完成
        void onRecordFinishedListener();
    }

    public OnLongClickListener onLongClickListener;

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    /**
     * 点击监听器
     */
    public interface OnClickListener {
        void onClick();
    }

    public OnClickListener onClickListener;

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

}
