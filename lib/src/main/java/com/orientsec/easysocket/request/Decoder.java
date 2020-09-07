package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

public interface Decoder<R> {
    R decode(@NonNull Packet packet) throws Exception;
}
