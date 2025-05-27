package com.orientsec.easysocket;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

public class EasyRunner {
    final Handler mHandler;

    public EasyRunner() {
        HandlerThread handlerThread = new HandlerThread("EasyMain");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    public void post(Runnable r) {
        mHandler.post(r);
    }

    public void postDelayed(Runnable r, Object token, long delayMillis) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mHandler.postDelayed(r, token, delayMillis);
        } else {
            mHandler.postAtTime(r, token, System.currentTimeMillis() + delayMillis);
        }
    }

    public void remove(Runnable r, Object token) {
        mHandler.removeCallbacks(r, token);
    }

}
