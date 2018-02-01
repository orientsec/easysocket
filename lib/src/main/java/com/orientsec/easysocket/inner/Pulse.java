package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Message;
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
public class Pulse implements Runnable {
    private AbstractConnection context;

    private ScheduledFuture future;

    private AtomicInteger lostTimes = new AtomicInteger();

    private Message pulseMessage;

    Pulse(AbstractConnection context) {
        this.context = context;
    }

    public void feed() {
        lostTimes.set(0);
    }

    public synchronized void start() {
        Options options = context.options;
        int rate = options.getPulseRate();
        future = options.getExecutorService()
                .scheduleAtFixedRate(this, rate, rate, TimeUnit.SECONDS);
        pulseMessage = new Message();
        pulseMessage.setMessageType(MessageType.PULSE);
        pulseMessage.setBodyBytes(options.getProtocol().pulseData());
    }

    synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (pulseMessage != null) {
            pulseMessage = null;
        }
        lostTimes.set(0);
    }

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
            Logger.w("pulse failed times up, invalid connection!");
            context.disconnect();
        } else {
            Message message = pulseMessage;
            if (message != null) {
                context.onPulse(message);
            }
        }
    }
}
