package com.orientsec.easysocket.utils;

import android.util.Log;

import com.orientsec.easysocket.Options;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2018/01/09 10:04
 * Author: Fredric
 * coding is art not science
 */
public class Logger {
    private static final String TAG = "EasySocket";

    public static void e(String msg) {
        if (Options.isDebug()) {
            Log.e(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (Options.isDebug()) {
            Log.i(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (Options.isDebug()) {
            Log.w(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (Options.isDebug()) {
            Log.d(TAG, msg);
        }
    }
}
