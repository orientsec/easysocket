package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

public interface Decoder<R> {
    @NonNull
    Result<R> decode(@NonNull Packet packet);
}
