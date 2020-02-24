package com.orientsec.easysocket;

public interface PulseHandler<T> {
    byte[] pulseData();

    boolean onPulse(T body);
}
