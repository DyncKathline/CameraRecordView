package com.kathline.cameralib;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.kathline.cameralib.constant.Capture;
import com.kathline.cameralib.constant.Code;
import com.kathline.cameralib.constant.Key;
import com.kathline.cameralib.utils.FileUtil;
import com.kathline.cameralib.view.CameraView;
import com.kathline.cameralib.view.CircleButtonView;

import java.io.File;

public class CameraActivity extends AppCompatActivity {

    private CameraView mCameraView;
    private RelativeLayout mRlCoverView;
    private ProgressBar mPbProgress;
    private String applicationName = "";
    private String cameraPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            applicationName = getString(R.string.app_name);
            PackageManager packageManager = getApplicationContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        toCustomCamera();
    }

    private void toCustomCamera() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            // 始终允许窗口延伸到屏幕短边上的刘海区域
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_camera);
        mCameraView = findViewById(R.id.camera_view);
        mPbProgress = findViewById(R.id.pb_progress);
        mCameraView.enableCameraTip(Setting.enableCameraTip);
        if (Setting.cameraCoverView != null && Setting.cameraCoverView.get() != null) {
            View coverView = Setting.cameraCoverView.get();
            mRlCoverView = findViewById(R.id.rl_cover_view);
            coverView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRlCoverView.addView(coverView);
        }
        initCustomCamera();
    }

    private int getFeature() {
        switch (Setting.captureType) {
            case Capture.ALL:
                return CircleButtonView.BUTTON_STATE_BOTH;
            case Capture.IMAGE:
                return CircleButtonView.BUTTON_STATE_ONLY_CAPTURE;
            default:
                return CircleButtonView.BUTTON_STATE_ONLY_RECORDER;
        }
    }

    private void initCustomCamera() {
        //视频存储路径
        if (FileUtil.beforeAndroidTen()) {
            CameraInterface.getInstance().setSaveVideoPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + applicationName);
        } else {
            CameraInterface.getInstance().setSaveVideoPath(getExternalFilesDir(Environment.DIRECTORY_DCIM) + File.separator + applicationName);
        }
        mCameraView.setFeatures(getFeature());
        mCameraView.setMediaQuality(Setting.RECORDING_BIT_RATE);
        if (Setting.cameraCoverView != null && Setting.cameraCoverView.get() != null) {
            mCameraView.setCameraPreViewListener(new CameraView.CameraPreViewListener() {

                @Override
                public void start(int type) {
                    mRlCoverView.setVisibility(View.GONE);
                }

                @Override
                public void stop(int type) {
                    mRlCoverView.setVisibility(View.VISIBLE);
                }
            });
        }
        //JCameraView监听
        mCameraView.setCameraListener(new CameraView.CameraListener() {
            @Override
            public void captureSuccess(Bitmap bitmap) {
                //获取图片bitmap
                String path;
                if (FileUtil.beforeAndroidTen()) {
                    path = FileUtil.saveBitmap(applicationName, bitmap);
                } else {
                    path = FileUtil.saveBitmapAndroidQ(CameraActivity.this, applicationName, bitmap);
                }
                Intent intent = new Intent();
                intent.putExtra(Key.EXTRA_RESULT_CAPTURE_IMAGE_PATH, path);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void recordSuccess(final String url, Bitmap firstFrame, long duration) {
                //获取视频路径
                if (FileUtil.beforeAndroidTen()) {
                    //String path = FileUtil.saveBitmap(applicationName, firstFrame);
                    Intent intent = new Intent();
                    //intent.putExtra(Key.EXTRA_RESULT_CAPTURE_IMAGE_PATH, path);
                    intent.putExtra(Key.EXTRA_RESULT_CAPTURE_VIDEO_PATH, url);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    mPbProgress.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //String path = FileUtil.saveBitmap(applicationName, firstFrame);
                            final String resUrl = FileUtil.copy2DCIMAndroidQ(CameraActivity.this, url, applicationName);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPbProgress.setVisibility(View.GONE);
                                    if (!isFinishing()) {
                                        Intent intent = new Intent();
                                        //intent.putExtra(Key.EXTRA_RESULT_CAPTURE_IMAGE_PATH, path);
                                        intent.putExtra(Key.EXTRA_RESULT_CAPTURE_VIDEO_PATH, resUrl);
                                        setResult(RESULT_OK, intent);
                                        finish();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }
        });
        mCameraView.setReturnListener(new CameraView.ReturnListener() {
            @Override
            public void onReturn() {
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏显示
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    protected void onDestroy() {
        if (Setting.cameraCoverView != null) Setting.cameraCoverView.clear();
        Setting.cameraCoverView = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }
        if (resultCode == RESULT_OK && Code.REQUEST_CAMERA == requestCode && cameraPath != null) {
            Intent intent = new Intent();
            intent.putExtra(Key.EXTRA_RESULT_CAPTURE_IMAGE_PATH, cameraPath);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}