package com.orientsec.easysocket.client;

import com.orientsec.easysocket.error.EasyException;
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

    private String name;

    private final Logger logger;

    protected Looper(Logger logger) {
        this.logger = logger;
    }

    /**
     * 启动
     */
    public synchronized void start() {
        if (!stop) {
            String name = getClass().getSimpleName();
            this.name = name;
            thread = new Thread(this, name);
            stop = false;
            loopTimes = 0;
            thread.start();
            logger.i(name + " is starting.");
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
            logger.i(name + " error.", e);
        } finally {
            logger.i(name + " is shutting down.");
            loopFinish();
        }
    }

    public long getLoopTimes() {
        return loopTimes;
    }

    protected abstract void beforeLoop() throws IOException, EasyException;

    protected abstract void runInLoopThread() throws IOException, EasyException;

    protected abstract void loopFinish();

    public synchronized void shutdown() {
        if (thread != null && !stop) {
            stop = true;
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isRunning() {
        return !stop;
    }
}
