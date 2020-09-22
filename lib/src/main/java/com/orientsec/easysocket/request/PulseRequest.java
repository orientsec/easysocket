package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

public class PulseRequest extends Request<Boolean> {
    private final Request<Boolean> request;

    public PulseRequest(Request<Boolean> request) {
        super(request.flag | PULSE);
        this.request = request;
    }

    @NonNull
    @Override
    public byte[] encode(int sequenceId) throws Exception {
        return request.encode(sequenceId);
    }

    @NonNull
    @Override
    public Boolean decode(@NonNull Packet data) throws Exception {
        return request.decode(data);
    }
}
