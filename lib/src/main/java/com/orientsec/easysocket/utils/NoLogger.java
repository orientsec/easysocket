package com.orientsec.easysocket.utils;

/**
 * NoLogger is a no-op implementation of the Logger interface.
 * This class provides empty method implementations for all logging levels,
 * effectively disabling logging when used.
 */
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
    public void d(String msg, Throwable t) {
    }
}
