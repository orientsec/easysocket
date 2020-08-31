package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.inner.PacketType;

class MyHead extends HeadParser.Head {
    private int taskId;
    private PacketType packetType;

    int getTaskId() {
        return taskId;
    }

    PacketType getPacketType() {
        return packetType;
    }

    MyHead(int packetSize, int taskId, PacketType packetType) {
        super(packetSize);
        this.taskId = taskId;
        this.packetType = packetType;
    }

}
