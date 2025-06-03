package com.orientsec.easysocket.utils;

import android.util.Log;

/**
 * AndroidLogger is a utility class that implements the Logger interface for logging messages
 * on the Android platform. It provides methods to log messages at different levels (error,
 * info, warning, debug) and supports detailed logging with thread information and a custom suffix.
 */
class AndroidLogger implements Logger {
    // The default tag used for logging
    private static final String TAG = "EasySocket";

    // A custom suffix appended to log messages
    private final String suffix;

    // Minimum log level to output
    private final int minLogLevel;

    /**
     * Constructs an AndroidLogger instance with the specified options and suffix.
     *
     * @param suffix      A custom suffix to append to log messages.
     * @param minLogLevel The minimum log level to output (e.g., Log.DEBUG, Log.INFO).
     */
    public AndroidLogger(String suffix, int minLogLevel) {
        this.suffix = suffix;
        this.minLogLevel = minLogLevel;
    }

    /**
     * Formats the log message by appending the suffix and thread information if detailed logging is enabled.
     *
     * @param msg The original log message.
     * @return The formatted log message.
     */
    private String formatMsg(String msg) {
        return msg + suffix +
                "  Thread:[" + Thread.currentThread().getName() + "]";
    }

    /**
     * Checks if the log level is enabled for output.
     *
     * @param level The log level to check.
     * @return True if the log level is enabled, false otherwise.
     */
    private boolean isIgnored(int level) {
        return level < minLogLevel;
    }

    @Override
    public void e(String msg) {
        if (isIgnored(Log.ERROR)) return;
        Log.e(TAG, formatMsg(msg));
    }

    @Override
    public void e(String msg, Throwable t) {
        if (isIgnored(Log.ERROR)) return;
        Log.e(TAG, formatMsg(msg), t);
    }

    @Override
    public void i(String msg) {
        if (isIgnored(Log.INFO)) return;
        Log.i(TAG, formatMsg(msg));
    }

    @Override
    public void i(String msg, Throwable t) {
        if (isIgnored(Log.INFO)) return;
        Log.i(TAG, formatMsg(msg), t);
    }

    @Override
    public void w(String msg) {
        if (isIgnored(Log.WARN)) return;
        Log.w(TAG, formatMsg(msg));
    }

    @Override
    public void w(String msg, Throwable t) {
        if (isIgnored(Log.WARN)) return;
        Log.w(TAG, formatMsg(msg), t);
    }

    @Override
    public void d(String msg) {
        if (isIgnored(Log.DEBUG)) return;
        Log.d(TAG, formatMsg(msg));
    }

    @Override
    public void d(String msg, Throwable t) {
        if (isIgnored(Log.DEBUG)) return;
        Log.d(TAG, formatMsg(msg), t);
    }
}