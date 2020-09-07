package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

public interface Encoder {
    @NonNull
    byte[] encode(int sequenceId) throws Exception;
}
