package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.utils.Logger;

public interface PacketHandler {
    /**
     * 接收分发消息
     *
     * @param packet 消息体
     */
    void handlePacket(@NonNull Packet<?> packet);

    class EmptyPacketHandler implements PacketHandler {

        @Override
        public void handlePacket(@NonNull Packet<?> packet) {
            Logger.i("Unhandled packet." + packet);
        }
    }
}
