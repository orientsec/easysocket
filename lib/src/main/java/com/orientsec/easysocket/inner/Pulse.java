package com.orientsec.easysocket.inner;

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
public class Pulse implements Runnable {
    private AbstractConnection context;

    private ScheduledFuture future;

    private AtomicInteger lostTimes = new AtomicInteger();

    private SendMessage pulseMessage;

    public Pulse(AbstractConnection context) {
        this.context = context;
    }

    public void feed() {
        lostTimes.set(0);
    }

    public synchronized void start() {
        future = context.executorService.schedule(this, context.options.getPulseRate(), TimeUnit.SECONDS);
        pulseMessage = new SendMessage();
        pulseMessage.setBodyBytes(context.options.getProtocol().pulseData());
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (pulseMessage != null) {
            pulseMessage.invalid();
            pulseMessage = null;
        }
    }

    @Override
    public void run() {
        if (lostTimes.getAndAdd(1) > context.options.getPulseLostTimes()) {
            Logger.w("pulse failed times up, invalid connection!");
            lostTimes.set(0);
            context.disconnect();
        } else {
            context.onPulse(pulseMessage);
        }
    }
}
