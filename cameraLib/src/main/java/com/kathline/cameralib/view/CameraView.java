package com.kathline.cameralib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import com.kathline.cameralib.utils.BitmapUtils;
import com.kathline.cameralib.CameraInterface;
import com.kathline.cameralib.CameraSourcePreview;
import com.kathline.cameralib.utils.FileUtil;
import com.kathline.cameralib.FrameMetadata;
import com.kathline.cameralib.GraphicOverlay;
import com.kathline.cameralib.MLKit;
import com.kathline.cameralib.R;
import com.kathline.cameralib.Setting;
import com.kathline.cameralib.VisionImageProcessor;
import com.kathline.cameralib.constant.Capture;
import com.kathline.cameralib.utils.ThreadUtil;

import java.nio.ByteBuffer;

public class CameraView extends FrameLayout {

    private static final String TAG = "CameraView";
    private View view;
    private CameraSourcePreview mPreviewView;
    private GraphicOverlay mGraphicOverlay;
    private ImageView mImgSwitchCamera;
    private TextView mTvTip;
    private ReturnButton mIvReturn;
    private CircleButtonView mCircleView;
    private SurfaceView mSvRecordVideo;
    private ImageView mImgPhoto;
    private TypeButton mIvRecordVideoCancel;
    private TypeButton mIvRecordVideoConfirm;
    private ConstraintLayout mClPhoto;

    private MLKit mlKit;
    private String videoFilePath = "";
    private Bitmap firstFrame;
    private Bitmap mBitmap;
    private String tipStr = "";
    private boolean isShowTip = true;
    private ThreadUtil threadUtil;

    public CameraView(@NonNull Context context) {
        super(context);
        init();
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        view = LayoutInflater.from(getContext()).inflate(R.layout.camera_view, null);
        addView(view);
        mPreviewView = view.findViewById(R.id.preview_view);
        mGraphicOverlay = view.findViewById(R.id.graphic_overlay);
        mImgSwitchCamera = view.findViewById(R.id.img_switch_camera);
        mTvTip = view.findViewById(R.id.tv_tip);
        mIvReturn = view.findViewById(R.id.iv_return);
        mCircleView = view.findViewById(R.id.circle_view);
        mSvRecordVideo = view.findViewById(R.id.sv_record_video);
        mImgPhoto = view.findViewById(R.id.img_photo);
        mIvRecordVideoCancel = view.findViewById(R.id.iv_record_video_cancel);
        mIvRecordVideoConfirm = view.findViewById(R.id.iv_record_video_confirm);
        mClPhoto = view.findViewById(R.id.cl_photo);

        mTvTip.setText(getTipText());
        mCircleView.setFeatures(getFeatures());
        mCircleView.setMinTime(Setting.videoMinSecond);
        mCircleView.setMaxTime(Setting.recordDuration);

        mIvRecordVideoConfirm.setType(TypeButton.TYPE_CONFIRM);

        mlKit = new MLKit((FragmentActivity) getContext(), mPreviewView, mGraphicOverlay);
        mlKit.getCameraSource().setMachineLearningFrameProcessor(new VisionImageProcessor() {
            @Override
            public void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay) {

            }

            @Override
            public void processByteBuffer(ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) {
                firstFrame = BitmapUtils.getBitmap(data, frameMetadata);
            }

            @Override
            public void stop() {

            }
        });
        mImgSwitchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mlKit.switchCamera();
            }
        });
        mCircleView.setOnClickListener(new CircleButtonView.OnClickListener() {
            @Override
            public void onClick() {
                CameraInterface.getInstance().takePicture(mlKit.getCameraSource(), new CameraInterface.TakePictureCallback() {
                    @Override
                    public void captureResult(Bitmap bitmap, boolean isVertical) {
                        mBitmap = bitmap;
                        mTvTip.setVisibility(GONE);
                        mClPhoto.setVisibility(View.VISIBLE);
                        mImgPhoto.setVisibility(View.VISIBLE);
                        mSvRecordVideo.setVisibility(View.GONE);
                        mImgPhoto.setImageBitmap(bitmap);
                    }
                });
            }
        });
        threadUtil = new ThreadUtil();
        mCircleView.setOnLongClickListener(new CircleButtonView.OnLongClickListener() {

            private ThreadUtil.TimerRunnable task;
            private int mTiming = 0;

            @Override
            public void onLongClick() {
                CameraInterface.getInstance().startRecord(mlKit.getCameraSource(), mPreviewView.getSurfaceView().getHolder().getSurface());
                releaseTiming();
                if(isShowTip) {
                    task = new ThreadUtil.TimerRunnable() {
                        @Override
                        public boolean run() {
                            mTiming++;
                            StringBuilder sbTiming = new StringBuilder(mTiming + "");
                            if (mTiming < 10) {
                                sbTiming.insert(0, "0");
                            }
                            sbTiming.insert(0, "00:");
                            mTvTip.setText(sbTiming.toString());
                            return false;
                        }
                    };
                    threadUtil.schedule(task, 1000, 1000);
                }
            }

            private void releaseTiming() {
                mTiming = 0;
                if(task != null) {
                    threadUtil.getHandler().removeCallbacks(task.mLastTicker);
                }
            }

            @Override
            public void onNoMinRecord(long currentTime) {
                releaseTiming();
                mTvTip.setVisibility(VISIBLE);
                mTvTip.setText(getTipText());
                String tip = getContext().getResources().getString(R.string.recode_min_video_time_tip);
                Toast.makeText(getContext(), String.format(tip, currentTime/1000), Toast.LENGTH_SHORT).show();
                CameraInterface.getInstance().stopRecord(true, new CameraInterface.StopRecordCallback() {
                    @Override
                    public void recordResult(String url, Bitmap firstFrame) {

                    }
                });
            }

            @Override
            public void onRecordFinishedListener() {
                releaseTiming();
                CameraInterface.getInstance().stopRecord(false, new CameraInterface.StopRecordCallback() {
                    @Override
                    public void recordResult(String url, Bitmap firstFrame) {
                        Log.i(TAG, "recordResult: " + url);
                        videoFilePath = url;
                        mClPhoto.setVisibility(View.VISIBLE);
                        mImgPhoto.setVisibility(View.GONE);
                        mSvRecordVideo.setVisibility(View.VISIBLE);
                        CameraInterface.getInstance().playVideo(mSvRecordVideo, firstFrame, url);
                    }
                });
            }
        });
        mIvRecordVideoCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTvTip.setVisibility(VISIBLE);
                mTvTip.setText(getTipText());
                mClPhoto.setVisibility(View.GONE);
                if(mSvRecordVideo.getVisibility() == View.VISIBLE) {
                    CameraInterface.getInstance().stopVideo();
                    FileUtil.deleteFile(videoFilePath);
                }
            }
        });
        mIvRecordVideoConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mImgPhoto.getVisibility() == View.VISIBLE) {
                    if(cameraListener != null) {
                        cameraListener.captureSuccess(mBitmap);
                    }
                }else if(mSvRecordVideo.getVisibility() == View.VISIBLE) {
                    CameraInterface.getInstance().stopVideo();
                    if(cameraListener != null) {
                        cameraListener.recordSuccess(videoFilePath, firstFrame, mCircleView.getCurrentPlayTime());
                    }
                }
            }
        });
        mIvReturn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(returnListener != null) {
                    returnListener.onReturn();
                }
            }
        });
    }

    private String getTipText() {
        String imageTip = getContext().getString(R.string.tap_take_photo);
        String videoTip = getContext().getString(R.string.long_press_camera);
        if(!TextUtils.isEmpty(tipStr)) {
            return tipStr;
        }
        switch (Setting.captureType) {
            case Capture.ALL:
                String split = getContext().getString(R.string.tap_take_photo_long_press_camera_split);
                return imageTip + split + videoTip;
            case Capture.IMAGE:
                return imageTip;
            default:
                return videoTip;
        }
    }

    private int getFeatures() {
        switch (Setting.captureType) {
            case Capture.ALL:
                return CircleButtonView.BUTTON_STATE_BOTH;
            case Capture.IMAGE:
                return CircleButtonView.BUTTON_STATE_ONLY_CAPTURE;
            default:
                return CircleButtonView.BUTTON_STATE_ONLY_RECORDER;
        }
    }

    //设置CaptureButton功能（拍照和录像）
    public void setFeatures(int state) {
        this.mCircleView.setFeatures(state);
    }

    //设置录制质量
    public void setMediaQuality(int quality) {
        CameraInterface.getInstance().setMediaQuality(quality);
    }

    public void setMinDuration(long duration) {
        mCircleView.setMinTime(duration);
    }

    public void setDuration(long duration) {
        mCircleView.setMaxTime(duration);
    }

    public void setTip(String tip) {
        tipStr = tip;
        mTvTip.setText(tip);
    }

    public void enableCameraTip(boolean enable) {
        isShowTip = enable;
        mTvTip.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }

    public interface CameraListener {

        void captureSuccess(Bitmap bitmap);

        void recordSuccess(String url, Bitmap firstFrame, long duration);
    }

    private CameraListener cameraListener;

    public void setCameraListener(CameraListener listener) {
        cameraListener = listener;
    }

    public interface ReturnListener {
        void onReturn();
    }

    private ReturnListener returnListener;

    public void setReturnListener(ReturnListener listener) {
        returnListener = listener;
    }

    public interface CameraPreViewListener {
        void start(int type);

        void stop(int type);
    }

    private CameraPreViewListener cameraPreViewListener;

    public void setCameraPreViewListener(CameraPreViewListener listener) {
        cameraPreViewListener = listener;
    }

}
