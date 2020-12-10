package com.colin.mosaicdemo;

import android.app.Application;
import android.content.Context;

/**
 * create by colin
 * 2020/12/10
 */
public class MyApp extends Application {

    public static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }
}
