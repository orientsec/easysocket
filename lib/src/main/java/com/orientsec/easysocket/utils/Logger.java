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
    public static void e(String msg) {
        if (Options.isDebug()) {
            Log.e("Socket", msg);
        }
    }

    public static void i(String msg) {
        if (Options.isDebug()) {
            Log.i("Socket", msg);
        }
    }

    public static void w(String msg) {
        if (Options.isDebug()) {
            Log.w("Socket", msg);
        }
    }

    public static void d(String msg) {
        if (Options.isDebug()) {
            Log.d("Socket", msg);
        }
    }
}
