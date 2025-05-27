package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

public interface Encoder {
    @NonNull
    Result<byte[]> encode(int sequenceId);
}
