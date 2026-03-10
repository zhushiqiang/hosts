package com.github.carhosts.vpn.util;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "CarHostsVPN";
    private static boolean DEBUG = true;

    public static void d(String tag, String msg) {
        if (DEBUG) Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (DEBUG) Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (DEBUG) Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUG) Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (DEBUG) Log.e(tag, msg, tr);
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void i(String msg) {
        i(TAG, msg);
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void e(String msg, Throwable tr) {
        e(TAG, msg, tr);
    }
}
