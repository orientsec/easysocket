package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Packet;

public interface MessageHandler<T> {
    /**
     * 接收分发消息
     *
     * @param packet 消息体
     */
    void handleMessage(Packet<T> packet);
}
