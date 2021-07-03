package com.kathline.cameraview;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.kathline.cameralib.CameraInterface;
import com.kathline.cameralib.CameraSourcePreview;
import com.kathline.cameralib.FileUtil;
import com.kathline.cameralib.GraphicOverlay;
import com.kathline.cameralib.MLKit;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CameraSourcePreview mPreviewView;
    private GraphicOverlay mGraphicOverlay;
    private ImageView mImgSwitchCamera;
    private CircleButtonView mCircleView;
    private MLKit mlKit;
    private TypeButton mIvRecordVideoCancel;
    private TypeButton mIvRecordVideoConfirm;
    private ConstraintLayout mClPhoto;
    private ImageView mImgPhoto;
    private SurfaceView mSvRecordVideo;

    private String videoFilePath = "";
    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreviewView = findViewById(R.id.preview_view);
        mGraphicOverlay = findViewById(R.id.graphic_overlay);
        mImgSwitchCamera = findViewById(R.id.img_switch_camera);
        mCircleView = findViewById(R.id.circle_view);
        mClPhoto = findViewById(R.id.cl_photo);
        mSvRecordVideo = findViewById(R.id.sv_record_video);
        mImgPhoto = findViewById(R.id.img_photo);
        mIvRecordVideoCancel = findViewById(R.id.iv_record_video_cancel);
        mIvRecordVideoConfirm = findViewById(R.id.iv_record_video_confirm);

        mCircleView.setMaxTime(10);
        mIvRecordVideoConfirm.setType(TypeButton.TYPE_CONFIRM);

        String applicationName = getString(R.string.app_name);
        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (FileUtil.beforeAndroidTen()) {
            CameraInterface.getInstance().setSaveVideoPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + applicationName);
        } else {
            CameraInterface.getInstance().setSaveVideoPath(getExternalFilesDir(Environment.DIRECTORY_DCIM) + File.separator + applicationName);
        }

        mlKit = new MLKit(this, mPreviewView, mGraphicOverlay);
        mImgSwitchCamera.setOnClickListener(new View.OnClickListener() {
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
                        mClPhoto.setVisibility(View.VISIBLE);
                        mImgPhoto.setVisibility(View.VISIBLE);
                        mSvRecordVideo.setVisibility(View.GONE);
                        mImgPhoto.setImageBitmap(bitmap);
                    }
                });
            }
        });
        mCircleView.setOnLongClickListener(new CircleButtonView.OnLongClickListener() {
            @Override
            public void onLongClick() {
                CameraInterface.getInstance().startRecord(mlKit.getCameraSource(), mPreviewView.getSurfaceView().getHolder().getSurface());
            }

            @Override
            public void onNoMinRecord(int currentTime) {
                CameraInterface.getInstance().stopRecord(true, new CameraInterface.StopRecordCallback() {
                    @Override
                    public void recordResult(String url, Bitmap firstFrame) {

                    }
                });
            }

            @Override
            public void onRecordFinishedListener() {
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
        mIvRecordVideoCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClPhoto.setVisibility(View.GONE);
                if(mSvRecordVideo.getVisibility() == View.VISIBLE) {
                    CameraInterface.getInstance().stopVideo();
                    FileUtil.deleteFile(videoFilePath);
                }
            }
        });
        String finalApplicationName = applicationName;
        mIvRecordVideoConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mImgPhoto.getVisibility() == View.VISIBLE) {
                    //获取图片bitmap
                    String path;
                    if (FileUtil.beforeAndroidTen()) {
                        path = FileUtil.saveBitmap(finalApplicationName, mBitmap);
                    } else {
                        path = FileUtil.saveBitmapAndroidQ(MainActivity.this, finalApplicationName, mBitmap);
                    }
                }
            }
        });
    }
}