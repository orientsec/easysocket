package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2018/01/04 09:33
 * Author: Fredric
 * coding is art not science
 */
public abstract class Looper implements Runnable {
    private Thread thread;

    private volatile boolean stop;

    private long loopTimes = 0;

    private Exception exception;

    /**
     * 启动
     */
    public synchronized void start() {
        if (!stop) {
            String threadName = getClass().getSimpleName();
            thread = new Thread(this, threadName);
            stop = false;
            loopTimes = 0;
            thread.start();
            Logger.i(threadName + " is starting");
        }
    }

    @Override
    public final void run() {
        try {
            beforeLoop();
            while (!stop) {
                this.runInLoopThread();
                loopTimes++;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            exception = e;
        } finally {
            this.loopFinish(exception);
            exception = null;
            Logger.i("Looper " + Thread.currentThread().getName() + " is shutting down");
        }
    }

    public long getLoopTimes() {
        return loopTimes;
    }

    protected abstract void beforeLoop() throws IOException, EasyException;

    protected abstract void runInLoopThread() throws IOException, EasyException;

    protected abstract void loopFinish(Throwable t);

    public synchronized void shutdown() {
        if (thread != null && !stop) {
            stop = true;
            thread.interrupt();
            thread = null;
        }
    }

    protected synchronized void shutdownWithException(Exception e) {
        this.exception = e;
        shutdown();
    }

    public boolean isRunning() {
        return !stop;
    }
}
