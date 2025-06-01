package com.orientsec.easysocket;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.Executor;

/**
 * The `EasyExecutor` class is a custom implementation of the `Executor` interface.
 * It uses a `Handler` to execute and schedule tasks on a background thread.
 * This class provides methods to execute tasks immediately, schedule tasks with a delay,
 * and remove scheduled tasks.
 */
public class EasyExecutor implements Executor {
    /**
     * The `Handler` used to post and manage tasks on a background thread.
     */
    final Handler mHandler;

    /**
     * Constructs an `EasyExecutor` instance.
     * Initializes a `HandlerThread` named "EasyMain" and creates a `Handler` associated with its looper.
     */
    public EasyExecutor() {
        HandlerThread handlerThread = new HandlerThread("EasyMain");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Executes a task immediately by posting it to the `Handler`.
     *
     * @param r The `Runnable` task to be executed.
     */
    @Override
    public void execute(Runnable r) {
        mHandler.post(r);
    }

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param r           The `Runnable` task to be scheduled.
     * @param delayMillis The delay in milliseconds before the task is executed.
     */
    public void schedule(Runnable r, long delayMillis) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mHandler.postDelayed(r, delayMillis);
        } else {
            mHandler.postAtTime(r, System.currentTimeMillis() + delayMillis);
        }
    }

    /**
     * Removes a previously scheduled task from the `Handler`.
     *
     * @param r The `Runnable` task to be removed.
     */
    public void remove(Runnable r) {
        mHandler.removeCallbacks(r);
    }
}
