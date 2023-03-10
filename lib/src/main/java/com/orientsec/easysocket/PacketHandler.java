package com.orientsec.easysocket;

import androidx.annotation.NonNull;

public interface PacketHandler {
    /**
     * 接收分发消息
     *
     * @param packet 消息体
     */
    void handlePacket(@NonNull Packet packet);

}