package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.utils.Logger;

public interface PacketHandler<T> {
    /**
     * 接收分发消息
     *
     * @param packet 消息体
     */
    void handlePacket(@NonNull Packet<T> packet);

    class EmptyPacketHandler<T> implements PacketHandler<T> {

        @Override
        public void handlePacket(@NonNull Packet<T> packet) {
            Logger.i("Unhandled packet." + packet);
        }
    }
}
