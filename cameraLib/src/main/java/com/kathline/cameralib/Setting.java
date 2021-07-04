package com.kathline.cameralib;

import android.view.View;

import com.kathline.cameralib.constant.Capture;
import java.lang.ref.WeakReference;

public class Setting {
    public static long videoMinSecond = 0L;
    public static long videoMaxSecond = Long.MAX_VALUE;
    public static String captureType = Capture.ALL;
    public static int recordDuration = 15000;
    public static WeakReference<View> cameraCoverView = null;
    public static boolean enableCameraTip = true;
    public static int RECORDING_BIT_RATE = CameraInterface.MEDIA_QUALITY_MIDDLE;

    public static void clear() {
        videoMinSecond = 0L;
        videoMaxSecond = Long.MAX_VALUE;
        captureType = Capture.ALL;
        recordDuration = 15000;
        if (cameraCoverView != null) cameraCoverView.clear();
        cameraCoverView = null;
        enableCameraTip = true;
        RECORDING_BIT_RATE = CameraInterface.MEDIA_QUALITY_MIDDLE;
    }
}
