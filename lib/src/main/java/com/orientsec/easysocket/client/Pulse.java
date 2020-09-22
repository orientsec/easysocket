package com.orientsec.easysocket.client;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.request.Callback;
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
    static final int PULSE = 201;

    private final AbstractSocketClient socketClient;

    private final Options options;

    private final Session session;

    private final EventManager eventManager;

    private final AtomicInteger lostTimes = new AtomicInteger();

    private final Executor codecExecutor;

    private final Logger logger;

    Pulse(AbstractSocketClient socketClient, Session session, EventManager eventManager) {
        this.socketClient = socketClient;
        this.session = session;
        this.eventManager = eventManager;
        options = socketClient.getOptions();
        codecExecutor = options.getCodecExecutor();
        logger = options.getLogger();
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
        eventManager.publish(PULSE, options.getPulseRate());
    }

    /**
     * 停止心跳，连接断开后调用
     */
    void stop() {
        eventManager.remove(PULSE);
    }

    /**
     * 发送一次心跳。
     */
    void pulse() {
        if (lostTimes.getAndAdd(1) > options.getPulseLostTimes()) {
            //心跳失败超过上限后断开连接
            logger.e("Pulse failed times up, session invalid.");
            session.close(Errors.connectError(ErrorCode.PULSE_TIME_OUT, "Pulse time out."));
        } else {
            Request<Boolean> pulseRequest = socketClient.getPulseRequest();
            if (!pulseRequest.isPulse()) {
                pulseRequest = new PulseRequest(pulseRequest);
            }
            socketClient.buildTask(pulseRequest, callback).execute();
            start();
        }
    }

    //发送心跳消息
    Callback<Boolean> callback = new Callback.EmptyCallback<Boolean>() {
        @Override
        public void onSuccess(@NonNull Boolean res) {
            if (res) {
                feed();
            }
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
                boolean success = socketClient.getPulseDecoder().decode(packet);
                if (success) {
                    feed();
                }
            } catch (Exception e) {
                logger.e("Server pulse failed!", e);
            }
        });
    }

}