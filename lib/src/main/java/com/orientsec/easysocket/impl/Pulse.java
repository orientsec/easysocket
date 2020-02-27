package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.PulseHandler;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.exception.Event;
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
public class Pulse<T> implements PacketHandler<T>, Runnable {
    private SocketConnection<T> connection;

    private Options<T> options;

    private PulseHandler<T> pulseHandler;

    private ScheduledFuture future;

    private AtomicInteger lostTimes = new AtomicInteger();

    Pulse(SocketConnection<T> connection) {
        this.connection = connection;
        this.options = connection.options;
        this.pulseHandler = options.getPulseHandler();
    }

    /**
     * 喂狗
     */
    private void feed(boolean flag) {
        if (flag) {
            lostTimes.set(0);
        } else {
            Logger.e("Pulse failed!");
        }
    }

    /**
     * 启动心跳，每一次连接建立成功后调用
     */
    synchronized void start() {
        int rate = options.getPulseRate();
        future = options.getScheduledExecutor()
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
            future = options.getScheduledExecutor()
                    .scheduleAtFixedRate(this, 0, options.getPulseRate(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        if (lostTimes.getAndAdd(1) > options.getPulseLostTimes()) {
            //心跳失败超过上限后断开连接
            Logger.w("pulse failed times up, invalid connection!");
            connection.disconnect(Event.PULSE_OVER_TIME);
        } else {
            //发送心跳消息
            Callback<Boolean> callback = new Callback.EmptyCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean res) {
                    feed(res);
                }
            };
            connection.buildTask(new PulseRequest<>(pulseHandler), callback)
                    .execute();
        }
    }

    @Override
    public void handlePacket(Packet<T> packet) {
        feed(pulseHandler.onPulse(packet.getBody()));
    }

    private static class PulseRequest<T> extends Request<T, Void, Boolean> {
        private PulseHandler<T> pulseHandler;

        PulseRequest(PulseHandler<T> pulseHandler) {
            this.pulseHandler = pulseHandler;
        }

        @Override
        public byte[] encode(int sequenceId) {
            return pulseHandler.pulseData();
        }

        @Override
        public Boolean decode(T data) {
            return pulseHandler.onPulse(data);
        }
    }

}
