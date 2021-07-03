package com.kathline.cameralib;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.io.IOException;
import java.util.List;

public class MLKit implements LifecycleObserver {

    private static final String TAG = "MLKit";
    private FragmentActivity activity;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;

    boolean isOpenLight = false;

    public MLKit(FragmentActivity activity, CameraSourcePreview preview, GraphicOverlay graphicOverlay) {
        this.activity = activity;
        this.preview = preview;
        this.graphicOverlay = graphicOverlay;
        activity.getLifecycle().addObserver(this);
        onCreate();
    }

    public void onCreate() {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        createCameraSource();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        Log.d(TAG, "onStart");
        createCameraSource();
        startCameraSource();
        CameraInterface.getInstance().registerSensorManager(activity);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Log.d(TAG, "onStop");
        preview.stop();
        CameraInterface.getInstance().unregisterSensorManager(activity);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    public CameraSource getCameraSource() {
        return cameraSource;
    }

    public void switchCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        if (numberOfCameras == 1) {
            return;
        }
        if(cameraSource != null) {
            if(cameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_FRONT) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            }
        }
        preview.stop();
        startCameraSource();
    }

    public boolean hasLight() {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * 开关闪关灯
     */
    public void switchLight() {
        if (hasLight()) {
            if (isOpenLight) {
                closeTorch();
            } else {
                openTorch();
            }
            isOpenLight = !isOpenLight;
        }
    }

    public void openTorch() {
        if(cameraSource != null) {
            cameraSource.setTorch(true);
        }
    }

    public void closeTorch() {
        if(cameraSource != null) {
            cameraSource.setTorch(false);
        }
    }

    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(activity, graphicOverlay);
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            requirePermission(new CallBack() {
                @Override
                public void call() {
                    try {
                        if (preview == null) {
                            Log.d(TAG, "resume: Preview is null");
                        }
                        if (graphicOverlay == null) {
                            Log.d(TAG, "resume: graphOverlay is null");
                        }
                        preview.start(cameraSource, graphicOverlay);
                        cameraSource.setOnCameraListener(new CameraSource.OnCameraListener() {
                            @Override
                            public void open(Camera camera) {
                                new GestureDetectorUtil(preview, camera);
                            }
                        });
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to start camera source.", e);
                        cameraSource.release();
                        cameraSource = null;
                    }
                }
            });
        }
    }

    private interface CallBack {
        void call();
    }

    private void requirePermission(CallBack callBack) {
        PermissionUtil.getInstance().with(activity).requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionUtil.PermissionListener() {
            @Override
            public void onGranted() {
                if(callBack != null) {
                    callBack.call();
                }
            }

            @Override
            public void onDenied(List<String> deniedPermission) {
                PermissionUtil.getInstance().showDialogTips(activity, deniedPermission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        activity.finish();
                    }
                });
            }

            @Override
            public void onShouldShowRationale(List<String> deniedPermission) {
//                requirePermission(callBack);
            }
        });
    }
}
