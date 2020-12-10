package com.colin.mosaicdemo.util;

import android.util.Log;

/**
 * create by colin
 * 2020/12/10
 */
public class Logger {

    private final String tag = "Logger";

    public void debug(String msg) {
        Log.d(tag, msg);
    }

    public void info(String msg) {
        Log.i(tag, msg);
    }

    public void warning(String msg) {
        Log.w(tag, msg);
    }
}
