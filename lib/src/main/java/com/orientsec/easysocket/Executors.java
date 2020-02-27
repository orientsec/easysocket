package com.orientsec.easysocket;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;

public class Executors {
    public static final ScheduledExecutorService scheduledExecutor
            = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    public static final ExecutorService managerExecutor
            = new ThreadPoolExecutor(0, 4,
            30L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    public static final ExecutorService codecExecutor
            = new ThreadPoolExecutor(0, 4,
            60L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    public static final Executor mainThreadExecutor = new Executor() {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    };
}
