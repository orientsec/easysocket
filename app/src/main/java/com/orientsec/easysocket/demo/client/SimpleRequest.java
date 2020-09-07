package com.orientsec.easysocket.demo.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.request.Request;

import java.nio.ByteBuffer;

public class SimpleRequest extends Request<String> {
    private int cmd;
    private Session session;
    private String param;
    private boolean init;

    SimpleRequest(String param, Session session) {
        this.cmd = 2;
        this.session = session;
        this.param = param;
    }

    public SimpleRequest(String param, int cmd, Session session) {
        this.cmd = cmd;
        this.session = session;
        this.param = param;
    }

    public SimpleRequest(String param, int cmd, boolean init, Session session) {
        this.cmd = cmd;
        this.session = session;
        this.init = init;
        this.param = param;
    }

    @Override
    public boolean isInitialize() {
        return init;
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