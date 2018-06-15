package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.utils.Logger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 13:21
 * Author: Fredric
 * coding is art not science
 * <p>
 * 心跳管理器
 */
public class Pulse<T> implements Runnable {
    private AbstractConnection<T> context;

    private ScheduledFuture future;

    private AtomicInteger lostTimes = new AtomicInteger();

    Pulse(AbstractConnection<T> context) {
        this.context = context;
    }

    /**
     * 喂狗
     */
    public void feed() {
        lostTimes.set(0);
    }

    /**
     * 启动心跳，每一次连接建立成功后调用
     */
    public synchronized void start() {
        Options options = context.options;
        int rate = options.getPulseRate();
        future = options.getExecutorService()
                .scheduleAtFixedRate(this, rate, rate, TimeUnit.SECONDS);

    }

    /**
     * 停止心跳，连接断开后调用
     */
    synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        lostTimes.set(0);
    }

    /**
     * 发送一次心跳, 应用从后台切换到前台，主动发送一次心跳
     */
    synchronized void pulseOnce() {
        if (future != null) {
            future.cancel(false);
            future = context.options.getExecutorService()
                    .scheduleAtFixedRate(this, 0, context.options.getPulseRate(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        if (lostTimes.getAndAdd(1) > context.options.getPulseLostTimes()) {
            //心跳失败超过上限后断开连接
            Logger.w("pulse failed times up, invalid connection!");
            context.disconnect(3);
        } else {
            //发送心跳消息
            context.onPulse(context.buildMessage(MessageType.PULSE));
        }
    }
}
