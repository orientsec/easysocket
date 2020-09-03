package com.orientsec.easysocket.demo.client;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.inner.PacketType;

import java.nio.ByteBuffer;

public class MyHeadParser implements HeadParser {

    @Override
    public int headSize() {
        return 12;
    }

    @Override
    @NonNull
    public MyHead parseHead(@NonNull byte[] header) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(header);
        int bodyLen = byteBuffer.getInt();
        int taskId = byteBuffer.getInt();
        int cmd = byteBuffer.getInt();
        PacketType packetType;
        if (cmd == 0) {
            packetType = PacketType.PULSE;
        } else {
            packetType = PacketType.RESPONSE;
        }
        return new MyHead(bodyLen, taskId, packetType);
    }

    @Override
    @NonNull
    public Packet decodePacket(@NonNull Head head, @NonNull byte[] bodyBytes) {
        MyHead myHead = (MyHead) head;
        return new Packet(myHead.getPacketType(), myHead.getTaskId(), bodyBytes);
    }
}