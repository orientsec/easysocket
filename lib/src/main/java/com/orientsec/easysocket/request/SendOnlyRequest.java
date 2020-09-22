package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

public class SendOnlyRequest<R> extends Request<R> {
    private final Encoder encoder;

    public SendOnlyRequest(int flag) {
        super(flag | NO_RESPONSE);
        encoder = null;
    }

    public SendOnlyRequest(int flag, Encoder encoder) {
        super(flag | NO_RESPONSE);
        this.encoder = encoder;
    }

    public SendOnlyRequest(Encoder encoder) {
        super(NO_RESPONSE);
        this.encoder = encoder;
    }

    public SendOnlyRequest() {
        super(NO_RESPONSE);
        encoder = null;
    }

    @NonNull
    @Override
    public byte[] encode(int sequenceId) throws Exception {
        if (encoder != null) {
            return encoder.encode(sequenceId);
        } else {
            return new byte[0];
        }
    }

    @NonNull
    @Override
    public final R decode(@NonNull Packet data) {
        throw new IllegalStateException("SendOnlyRequest not support decode action.");
    }
}
