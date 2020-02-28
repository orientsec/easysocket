package com.orientsec.easysocket;

public interface PulseHandler<T> {
    byte[] pulseData(int sequenceId);

    boolean onPulse(T body);

    final class EmptyPulseHandler<T> implements PulseHandler<T> {

        @Override
        public byte[] pulseData(int sequenceId) {
            return new byte[0];
        }

        @Override
        public boolean onPulse(T body) {
            return true;
        }
    }
}
