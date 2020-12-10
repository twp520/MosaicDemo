package com.colin.mosaicdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.colin.mosaicdemo.art.DoubleExposureActivity;
import com.colin.mosaicdemo.mosaic.MainActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    public void goMosaic(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void goExposure(View v) {
        Intent intent = new Intent(this, DoubleExposureActivity.class);
        startActivity(intent);
    }
}