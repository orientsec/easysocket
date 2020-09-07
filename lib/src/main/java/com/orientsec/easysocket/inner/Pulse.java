package com.orientsec.easysocket.inner;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.PulseRequest;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.utils.Logger;

import java.util.concurrent.Executor;
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
public class Pulse implements PacketHandler {

    private EasySocket easySocket;

    private final EventManager eventManager;

    private final AtomicInteger lostTimes = new AtomicInteger();

    private final Executor codecExecutor;

    private final Logger logger;

    Pulse(EasySocket easySocket) {
        this.easySocket = easySocket;
        eventManager = easySocket.getEventManager();
        codecExecutor = easySocket.getCodecExecutor();
        logger = easySocket.getLogger();
    }

    /**
     * 喂狗
     */
    private void feed() {
        lostTimes.set(0);
    }

    /**
     * 启动心跳，每一次连接建立成功后调用
     */
    void start() {
        int rate = easySocket.getPulseRate();
        eventManager.remove(Events.PULSE);
        eventManager.publish(Events.PULSE, rate);
    }

    /**
     * 停止心跳，连接断开后调用
     */
    void stop() {
        eventManager.remove(Events.PULSE);
        lostTimes.set(0);
    }

    /**
     * 发送一次心跳。
     */
    void pulse() {
        if (lostTimes.getAndAdd(1) > easySocket.getPulseLostTimes()) {
            //心跳失败超过上限后断开连接
            logger.e("Pulse failed times up, connection invalid.");
            eventManager.publish(Events.CONNECT_ERROR,
                    Errors.connectError(ErrorCode.PULSE_TIME_OUT, "Pulse time out."));
        } else {
            Request<?> request = easySocket.getPulseRequestProvider().get();
            Request<?> pulseRequest = new PulseRequest<>(request);
            easySocket.getConnection()
                    .buildTask(pulseRequest, callback)
                    .execute();
            start();
        }
    }

    //发送心跳消息
    Callback<Object> callback = new Callback.EmptyCallback<Object>() {
        @Override
        public void onSuccess(@NonNull Object res) {
            feed();
        }

        @Override
        public void onError(@NonNull Exception e) {
            logger.e("Client pulse failed!", e);
        }
    };

    @Override
    public void handlePacket(@NonNull Packet packet) {
        codecExecutor.execute(() -> {
            try {
                Decoder<?> decoder = easySocket.getPulseDecoderProvider().get();
                decoder.decode(packet);
                feed();
            } catch (Exception e) {
                logger.e("Server pulse failed!", e);
            }
        });
    }

}