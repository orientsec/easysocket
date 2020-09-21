package com.orientsec.easysocket.utils;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.SocketClient;

class NoLogger implements Logger {
    @Override
    public void e(String msg) {

    }

    @Override
    public void e(String msg, Throwable t) {

    }

    @Override
    public void i(String msg) {

    }

    @Override
    public void i(String msg, Throwable t) {

    }

    @Override
    public void w(String msg) {

    }

    @Override
    public void w(String msg, Throwable t) {

    }

    @Override
    public void d(String msg) {

    }

    @Override
    public void attach(@NonNull SocketClient socketClient) {

    }
}
