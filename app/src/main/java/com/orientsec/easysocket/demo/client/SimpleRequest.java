package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.request.Request;

import java.nio.ByteBuffer;

public class SimpleRequest extends Request<String> {
    private final int cmd;
    private final Session session;
    private final String param;

    SimpleRequest(String param, Session session) {
        this(2, param, session);
    }


    public SimpleRequest(int cmd, String param, Session session) {
        this.param = param;
        this.session = session;
        this.cmd = cmd;
    }

    @Override
    @NonNull
    public byte[] encode(int sequenceId) {
        byte[] body = param.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putInt(body.length);
        byteBuffer.putInt(sequenceId);
        //packet type
        byteBuffer.putInt(cmd);
        byteBuffer.putInt(session.getSessionId());

        byte[] head = byteBuffer.array();
        byte[] sendBytes = new byte[head.length + body.length];
        System.arraycopy(head, 0, sendBytes, 0, head.length);
        System.arraycopy(body, 0, sendBytes, head.length, body.length);
        return sendBytes;
    }

    @Override
    @NonNull
    public String decode(@NonNull Packet packet) {
        return new String((byte[]) packet.getBody());
    }
}