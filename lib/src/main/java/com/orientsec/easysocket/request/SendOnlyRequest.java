package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

public class SendOnlyRequest<R> extends Request<R> {
    private final Encoder encoder;

    public SendOnlyRequest(Encoder encoder) {
        this.encoder = encoder;
    }

    public SendOnlyRequest() {
        encoder = null;
    }

    @Override
    public final boolean isSendOnly() {
        return true;
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
