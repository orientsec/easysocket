package com.orientsec.easysocket.demo.client;

import com.orientsec.easysocket.Request;

import java.nio.ByteBuffer;

public class SimpleRequest extends Request<byte[], String, String> {
    private int cmd;
    private Session session;

    SimpleRequest(String out, Session session) {
        super(out);
        this.cmd = 2;
        this.session = session;
    }

    public SimpleRequest(String out, int cmd, Session session) {
        super(out);
        this.cmd = cmd;
        this.session = session;
    }

    public SimpleRequest(String out, int cmd, boolean init, Session session) {
        super(out);
        this.cmd = cmd;
        this.init = init;
        this.session = session;
    }

    @Override
    public byte[] encode(int sequenceId) {
        byte[] body = request.getBytes();
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
    public String decode(byte[] data) {
        return new String(data);
    }
}