package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

public class PulseRequest<R> extends Request<R> {
    private final Request<R> request;

    public PulseRequest(Request<R> request) {
        this.request = request;
    }

    @Override
    public boolean isSendOnly() {
        return request.isSendOnly();
    }

    @NonNull
    @Override
    public byte[] encode(int sequenceId) throws Exception {
        return request.encode(sequenceId);
    }

    @NonNull
    @Override
    public R decode(@NonNull Packet data) throws Exception {
        return request.decode(data);
    }
}
