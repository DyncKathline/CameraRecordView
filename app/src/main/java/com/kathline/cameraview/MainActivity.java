package com.kathline.cameraview;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.kathline.cameralib.CameraActivity;
import com.kathline.cameralib.Setting;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Setting.videoMinSecond = 3000;
                Setting.recordDuration = 10000;
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            }
        });
    }
}