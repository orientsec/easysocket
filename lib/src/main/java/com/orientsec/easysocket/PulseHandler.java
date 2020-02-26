package com.orientsec.easysocket;

public interface PulseHandler<T> {
    byte[] pulseData();

    boolean onPulse(T body);

    final class EmptyPulseHandler<T> implements PulseHandler<T> {

        @Override
        public byte[] pulseData() {
            return new byte[0];
        }

        @Override
        public boolean onPulse(T body) {
            return true;
        }
    }
}
