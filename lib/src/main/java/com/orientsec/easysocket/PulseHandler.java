package com.orientsec.easysocket;

import androidx.annotation.NonNull;

public interface PulseHandler {
    @NonNull
    byte[] pulseData(int sequenceId);

    boolean onPulse(@NonNull Packet packet);

}

final class EmptyPulseHandler implements PulseHandler {

    @Override
    @NonNull
    public byte[] pulseData(int sequenceId) {
        return new byte[0];
    }

    @Override
    public boolean onPulse(@NonNull Packet packet) {
        return true;
    }
}