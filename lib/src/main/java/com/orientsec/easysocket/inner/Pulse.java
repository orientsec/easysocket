package com.orientsec.easysocket.inner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.PulseHandler;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
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
public class Pulse implements PacketHandler, EventListener {

    private EasySocket easySocket;

    private final EventManager eventManager;

    private final PulseHandler pulseHandler;

    private final AtomicInteger lostTimes = new AtomicInteger();

    private final Executor codecExecutor;

    private final Logger logger;

    Pulse(EasySocket easySocket) {
        this.easySocket = easySocket;
        eventManager = easySocket.getEventManager();
        pulseHandler = easySocket.getPulseHandlerProvider().get(easySocket);
        codecExecutor = easySocket.getCodecExecutor();
        logger = easySocket.getLogger();
    }

    /**
     * 喂狗
     */
    private void feed(boolean flag) {
        if (flag) {
            lostTimes.set(0);
        } else {
            logger.e("Pulse failed!");
        }
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
     * 发送一次心跳, 应用从后台切换到前台，主动发送一次心跳
     */
    private void pulse() {
        if (lostTimes.getAndAdd(1) > easySocket.getPulseLostTimes()) {
            //心跳失败超过上限后断开连接
            logger.e("Pulse failed times up, connection invalid.");
            eventManager.publish(Events.STOP,
                    Errors.connectError(ErrorCode.PULSE_TIME_OUT, "Pulse time out."));
        } else {
            //发送心跳消息
            Callback<Boolean> callback = new Callback.EmptyCallback<Boolean>() {
                @Override
                public void onSuccess(@NonNull Boolean res) {
                    feed(res);
                }
            };
            easySocket.getConnection().buildTask(new PulseRequest(pulseHandler), callback)
                    .execute();
            start();
        }
    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        codecExecutor.execute(() -> feed(pulseHandler.onPulse(packet)));
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId == Events.PULSE) {
            pulse();
        }
    }
}

class PulseRequest extends Request<Boolean> {
    private PulseHandler pulseHandler;

    PulseRequest(PulseHandler pulseHandler) {
        this.pulseHandler = pulseHandler;
    }

    @Override
    @NonNull
    public byte[] encode(int sequenceId) {
        return pulseHandler.pulseData(sequenceId);
    }

    @Override
    @NonNull
    public Boolean decode(@NonNull Packet packet) {
        return pulseHandler.onPulse(packet);
    }
}