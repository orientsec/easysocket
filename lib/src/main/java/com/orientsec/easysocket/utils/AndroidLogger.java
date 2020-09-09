package com.orientsec.easysocket.utils;

import android.util.Log;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2018/01/09 10:04
 * Author: Fredric
 * coding is art not science
 */
class AndroidLogger implements Logger {
    private static final String TAG = "EasySocket";
    private final String tag;

    AndroidLogger(String name) {
        tag = TAG + '[' + name + ']';
    }

    @Override
    public void e(String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void e(String msg, Throwable t) {
        Log.e(tag, msg, t);
    }

    @Override
    public void i(String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void i(String msg, Throwable t) {
        Log.i(tag, msg, t);
    }

    @Override
    public void w(String msg) {
        Log.w(tag, msg);
    }

    @Override
    public void w(String msg, Throwable t) {
        Log.w(tag, msg, t);
    }

    @Override
    public void d(String msg) {
        Log.d(tag, msg);
    }
}
