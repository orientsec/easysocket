package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.impl.PacketType;

import java.nio.ByteBuffer;

public class MyHeadParser implements HeadParser<byte[]> {

    @Override
    public int headSize() {
        return 12;
    }

    @Override
    public MyHead parseHead(byte[] header) {
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
    public Packet<byte[]> decodePacket(Head head, byte[] bodyBytes) {
        MyHead myHead = (MyHead) head;
        return new Packet<>(myHead.getPacketType(), myHead.getTaskId(), bodyBytes);
    }
}