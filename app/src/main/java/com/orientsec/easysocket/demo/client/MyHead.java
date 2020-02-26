package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.impl.PacketType;

class MyHead implements HeadParser.Head {
    private int size;
    private int taskId;
    private PacketType packetType;

    int getTaskId() {
        return taskId;
    }

    PacketType getPacketType() {
        return packetType;
    }

    MyHead(int size, int taskId, PacketType packetType) {
        this.size = size;
        this.taskId = taskId;
        this.packetType = packetType;
    }

    @Override
    public int bodySize() {
        return size;
    }
}
