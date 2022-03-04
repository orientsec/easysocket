package com.orientsec.easysocket.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.SocketClient;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2018/01/09 10:04
 * Author: Fredric
 * coding is art not science
 */
class AndroidLogger implements Logger {
    private static final String TAG = "EasySocket";
    private final String suffix;
    private final Options options;

    public AndroidLogger(Options options, String suffix) {
        this.options = options;
        this.suffix = suffix;
    }

    private String formatMsg(String msg) {
        if (options.isDetailLog()) {
            return msg + suffix +
                    "  Thread:[" + Thread.currentThread().getName() + "]";
        } else {
            return msg;
        }
    }

    @Override
    public void e(String msg) {
        Log.e(TAG, formatMsg(msg));
    }

    @Override
    public void e(String msg, Throwable t) {
        Log.e(TAG, formatMsg(msg), t);
    }

    @Override
    public void i(String msg) {
        Log.i(TAG, formatMsg(msg));
    }

    @Override
    public void i(String msg, Throwable t) {
        Log.i(TAG, formatMsg(msg), t);
    }

    @Override
    public void w(String msg) {
        Log.w(TAG, formatMsg(msg));
    }

    @Override
    public void w(String msg, Throwable t) {
        Log.w(TAG, formatMsg(msg), t);
    }

    @Override
    public void d(String msg) {
        Log.d(TAG, formatMsg(msg));
    }

}
