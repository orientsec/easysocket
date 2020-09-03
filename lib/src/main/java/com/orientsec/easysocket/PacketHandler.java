package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.utils.AndroidLogger;
import com.orientsec.easysocket.utils.Logger;

public interface PacketHandler {
    /**
     * 接收分发消息
     *
     * @param packet 消息体
     */
    void handlePacket(@NonNull Packet packet);

}

final class EmptyPacketHandler implements PacketHandler {
    private Logger logger;

    public EmptyPacketHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void handlePacket(@NonNull Packet packet) {
        logger.i("Unhandled packet. " + packet);
    }
}
