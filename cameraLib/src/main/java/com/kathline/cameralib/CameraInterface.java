package com.kathline.cameralib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.kathline.cameralib.utils.AngleUtil;
import com.kathline.cameralib.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;

/**
 * 拍照录像功能
 */
public class CameraInterface {

    private static final String TAG = "CameraInterface";

    private volatile static CameraInterface mCameraInterface;

    public static void destroyCameraInterface() {
        if (mCameraInterface != null) {
            mCameraInterface = null;
        }
    }

    private Camera mCamera;
    private Camera.Parameters mParams;

    public static boolean isRecorder = false;
    private MediaRecorder mediaRecorder;
    private String videoFileName;
    private String saveVideoPath;
    private String videoFileAbsPath;
    private Bitmap videoFirstFrame = null;
    private MediaPlayer mMediaPlayer;
    private boolean isSurfaceCreated;

    private int angle = 0;
    private int cameraAngle = 90;//摄像头角度   默认为90度
    private int rotation = 0;

    public static final int TYPE_RECORDER = 0x090;
    public static final int TYPE_CAPTURE = 0x091;
    private int nowScaleRate = 0;
    private int recordScleRate = 0;

    //录制视频比特率
    public static final int MEDIA_QUALITY_SUPER = 84 * 100000;
    public static final int MEDIA_QUALITY_HIGH = 52 * 100000;
    public static final int MEDIA_QUALITY_MIDDLE = 28 * 100000;
    public static final int MEDIA_QUALITY_LOW = 14 * 100000;
    public static final int MEDIA_QUALITY_POOR = 8 * 100000;
    //视频质量
    private int mediaQuality = MEDIA_QUALITY_MIDDLE;
    private SensorManager sm = null;

    //获取CameraInterface单例
    public static synchronized CameraInterface getInstance() {
        if (mCameraInterface == null)
            synchronized (CameraInterface.class) {
                if (mCameraInterface == null)
                    mCameraInterface = new CameraInterface();
            }
        return mCameraInterface;
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (Sensor.TYPE_ACCELEROMETER != event.sensor.getType()) {
                return;
            }
            float[] values = event.values;
            angle = AngleUtil.getSensorAngle(values[0], values[1]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setSaveVideoPath(String saveVideoPath) {
        this.saveVideoPath = saveVideoPath;
        File file = new File(saveVideoPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void setZoom(float zoom, int type) {
        if (mCamera == null) {
            return;
        }
        if (mParams == null) {
            mParams = mCamera.getParameters();
        }
        if (!mParams.isZoomSupported()) {
            return;
        }
        try {
            int scaleRate = (int) (zoom);
            int maxZoom = mParams.getMaxZoom();
            switch (type) {
                case TYPE_RECORDER:
                    //如果不是录制视频中，上滑不会缩放
                    if (!isRecorder) {
                        return;
                    }
                    if (scaleRate <= maxZoom && scaleRate >= nowScaleRate && recordScleRate != scaleRate) {
                        if (mParams.isSmoothZoomSupported()) {
                            mCamera.startSmoothZoom(scaleRate);
                        } else {
                            if (scaleRate <= maxZoom) {
                                mParams.setZoom(scaleRate);
                            } else {
                                mParams.setZoom(maxZoom);
                            }
                            mCamera.setParameters(mParams);
                        }
                        recordScleRate = scaleRate;
                    }
                    break;
                case TYPE_CAPTURE:
                    if (isRecorder) {
                        return;
                    }
                    //每移动50个像素缩放一个级别
                    if (scaleRate < maxZoom) {
                        nowScaleRate += scaleRate;
                        if (nowScaleRate < 0) {
                            nowScaleRate = 0;
                        } else if (nowScaleRate > maxZoom) {
                            nowScaleRate = maxZoom;
                        }
                        if (mParams.isSmoothZoomSupported()) {
                            mCamera.startSmoothZoom(nowScaleRate);
                        } else {
                            mParams.setZoom(nowScaleRate);
                            mCamera.setParameters(mParams);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int getCameraScaleRate() {
        return mCamera.getParameters().getZoom();
    }

    public void setMediaQuality(int quality) {
        this.mediaQuality = quality;
    }

    private CameraInterface() {
        saveVideoPath = "";
    }

    private void setFlashModel() {
        mParams = mCamera.getParameters();
        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); //设置camera参数为Torch模式
        mCamera.setParameters(mParams);
    }

    /**
     * 拍照
     */
    private int nowAngle;
    private boolean safeToTakePicture = true;

    public void takePicture(CameraSource cameraSource, final TakePictureCallback callback) {
        mCamera = cameraSource.getCamera();
        if (mCamera == null) {
            return;
        }
        switch (cameraAngle) {
            case 90:
                nowAngle = Math.abs(angle + cameraAngle) % 360;
                break;
            case 270:
                nowAngle = Math.abs(cameraAngle - angle);
                break;
        }
//
        Log.i(TAG, angle + " = " + cameraAngle + " = " + nowAngle);
        if(safeToTakePicture) {
            safeToTakePicture = false;
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    safeToTakePicture = true;
                    mCamera.startPreview();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Matrix matrix = new Matrix();
                    if (cameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_BACK) {
                        matrix.setRotate(nowAngle);
                    } else if (cameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_FRONT) {
                        matrix.setRotate(360 - nowAngle);
                        matrix.postScale(-1, 1);
                    }

                    bitmap = createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    if (callback != null) {
                        if (nowAngle == 90 || nowAngle == 270) {
                            callback.captureResult(bitmap, true);
                        } else {
                            callback.captureResult(bitmap, false);
                        }
                    }
                }
            });
        }
    }

    //启动录像
    public void startRecord(CameraSource cameraSource, Surface surface) {
        mCamera = cameraSource.getCamera();
        if (mCamera == null) {
            return;
        }
//        mCamera.setPreviewCallback(null);
        final int nowAngle = (angle + 90) % 360;

        if (isRecorder) {
            return;
        }
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        mParams = mCamera.getParameters();
        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mCamera.setParameters(mParams);
        mCamera.unlock();
        mediaRecorder.reset();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        float videoRatio = -1;//视频宽高比
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
            CamcorderProfile profile = CamcorderProfile.get(cameraSource.getCameraFacing(), CamcorderProfile.QUALITY_HIGH);
            if (profile != null) {
                videoRatio = profile.videoFrameWidth * 1.0f / profile.videoFrameHeight;
            }
        }
        Camera.Size videoSize;
        if (mParams.getSupportedVideoSizes() == null) {
            videoSize = CameraParamUtil.getInstance().getPreviewSize(mParams.getSupportedPreviewSizes(), 800, videoRatio);
        } else {
            videoSize = CameraParamUtil.getInstance().getPreviewSize(mParams.getSupportedVideoSizes(), 800, videoRatio);
        }
        Log.i(TAG, "setVideoSize    width = " + videoSize.width + "height = " + videoSize.height);
        mediaRecorder.setVideoSize(videoSize.width, videoSize.height);
//        mediaRecorder.setVideoSize(640, 480);
//        if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
//            mediaRecorder.setOrientationHint(270);
//        } else {
//            mediaRecorder.setOrientationHint(nowAngle);
////            mediaRecorder.setOrientationHint(90);
//        }

        if (cameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_FRONT) {
            //手机预览倒立的处理
            if (cameraAngle == 270) {
                //横屏
                if (nowAngle == 0) {
                    mediaRecorder.setOrientationHint(180);
                } else if (nowAngle == 270) {
                    mediaRecorder.setOrientationHint(270);
                } else {
                    mediaRecorder.setOrientationHint(90);
                }
            } else {
                if (nowAngle == 90) {
                    mediaRecorder.setOrientationHint(270);
                } else if (nowAngle == 270) {
                    mediaRecorder.setOrientationHint(90);
                } else {
                    mediaRecorder.setOrientationHint(nowAngle);
                }
            }
        } else {
            mediaRecorder.setOrientationHint(nowAngle);
        }

//        fixme 暂时去掉 后期有问题再处理 没注释也不知道为啥要特殊处理华为
//        if (DeviceUtil.isHuaWeiRongyao()) {
//            mediaRecorder.setVideoEncodingBitRate(4 * 100000);
//        } else {
        mediaRecorder.setVideoEncodingBitRate(mediaQuality);
//        }
        mediaRecorder.setPreviewDisplay(surface);

        videoFileName = "VID_" + System.currentTimeMillis() + ".mp4";
        if (saveVideoPath.equals("")) {
//            saveVideoPath = Environment.getExternalStorageDirectory().getPath();
            saveVideoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        }
        videoFileAbsPath = saveVideoPath + File.separator + videoFileName;
        Log.i(TAG, "videoFileAbsPath: " + videoFileAbsPath);
        mediaRecorder.setOutputFile(videoFileAbsPath);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecorder = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.i(TAG, "startRecord IllegalStateException");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "startRecord IOException");
        } catch (RuntimeException e) {
            Log.i(TAG, "startRecord RuntimeException");
        }
    }

    //停止录像
    public void stopRecord(boolean isShort, StopRecordCallback callback) {
        if (!isRecorder) {
            return;
        }
        //视频录制恢复相机缩放
        setZoom(0, CameraInterface.TYPE_RECORDER);
        if (mediaRecorder != null) {
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setOnInfoListener(null);
            mediaRecorder.setPreviewDisplay(null);
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
                mediaRecorder = null;
                mediaRecorder = new MediaRecorder();
            } finally {
                if (mediaRecorder != null) {
                    mediaRecorder.release();
                }
                mediaRecorder = null;
                isRecorder = false;
            }
            if (isShort) {
                if (FileUtil.deleteFile(videoFileAbsPath)) {
                    callback.recordResult(null, null);
                }
                return;
            }
            //停顿200毫秒，确保写入数据结束完成
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String fileName = saveVideoPath + File.separator + videoFileName;
            callback.recordResult(fileName, videoFirstFrame);
        }
    }

    public interface StopRecordCallback {
        void recordResult(String url, Bitmap firstFrame);
    }

    public interface TakePictureCallback {
        void captureResult(Bitmap bitmap, boolean isVertical);
    }

    public void playVideo(SurfaceView surfaceView, Bitmap firstFrame, final String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        this.videoFirstFrame = firstFrame;
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
                mMediaPlayer = new MediaPlayer();
                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(@NonNull SurfaceHolder holder) {
                        isSurfaceCreated = true;
                        mMediaPlayer.setDisplay(surfaceView.getHolder());
                    }

                    @Override
                    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                    }

                    @Override
                    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                        isSurfaceCreated = false;
                    }
                });
                if(isSurfaceCreated) {
                    mMediaPlayer.setDisplay(surfaceView.getHolder());
                }
                try {
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(url);
                    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer
                        .OnVideoSizeChangedListener() {
                    @Override
                    public void
                    onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        updateVideoViewSize(surfaceView, mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                    }
                });
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mMediaPlayer.start();
                    }
                });
            }
        }).start();
    }

    public void stopVideo() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void updateVideoViewSize(SurfaceView surfaceView, float videoWidth, float videoHeight) {
        int width = surfaceView.getWidth();
        int height = (int) ((videoHeight*1f / videoWidth) * width);
        ViewGroup.MarginLayoutParams videoViewParam;
        videoViewParam = (ViewGroup.MarginLayoutParams) surfaceView.getLayoutParams();
        videoViewParam.width = width;
        videoViewParam.height = height;
//            videoViewParam.gravity = Gravity.CENTER;
        surfaceView.setLayoutParams(videoViewParam);
    }

    void registerSensorManager(Context context) {
        if (sm == null) {
            sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }
        sm.registerListener(sensorEventListener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager
                .SENSOR_DELAY_NORMAL);
    }

    void unregisterSensorManager(Context context) {
        if (sm == null) {
            sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }
        sm.unregisterListener(sensorEventListener);
        sm = null;
    }

    public int getCameraDisplayOrientation(Context context, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
}
