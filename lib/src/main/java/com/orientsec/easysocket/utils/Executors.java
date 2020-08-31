package com.orientsec.easysocket.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public interface Executors {

    ExecutorService connectExecutor
            = new ThreadPoolExecutor(0, 4,
            30L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    ExecutorService codecExecutor
            = new ThreadPoolExecutor(0, 4,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    Executor mainThreadExecutor = new Executor() {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    };
}
