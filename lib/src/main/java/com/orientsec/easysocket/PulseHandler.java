package com.orientsec.easysocket;

import androidx.annotation.NonNull;

public interface PulseHandler<T> {
    @NonNull
    byte[] pulseData(int sequenceId);

    boolean onPulse(@NonNull T body);

    final class EmptyPulseHandler<T> implements PulseHandler<T> {

        @Override
        @NonNull
        public byte[] pulseData(int sequenceId) {
            return new byte[0];
        }

        @Override
        public boolean onPulse(@NonNull T body) {
            return true;
        }
    }
}
