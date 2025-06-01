package com.orientsec.easysocket.utils;

import android.util.Log;

import com.orientsec.easysocket.Options;

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

    // Configuration options for controlling logging behavior
    private final Options options;

    /**
     * Constructs an AndroidLogger instance with the specified options and suffix.
     *
     * @param options The configuration options for logging behavior.
     * @param suffix  A custom suffix to append to log messages.
     */
    public AndroidLogger(Options options, String suffix) {
        this.options = options;
        this.suffix = suffix;
    }

    /**
     * Formats the log message by appending the suffix and thread information if detailed logging is enabled.
     *
     * @param msg The original log message.
     * @return The formatted log message.
     */
    private String formatMsg(String msg) {
        if (options.isDetailLog()) {
            return msg + suffix +
                    "  Thread:[" + Thread.currentThread().getName() + "]";
        } else {
            return msg;
        }
    }

    /**
     * Logs an error-level message.
     *
     * @param msg The message to log.
     */
    @Override
    public void e(String msg) {
        Log.e(TAG, formatMsg(msg));
    }

    /**
     * Logs an error-level message with an exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    @Override
    public void e(String msg, Throwable t) {
        Log.e(TAG, formatMsg(msg), t);
    }

    /**
     * Logs an info-level message.
     *
     * @param msg The message to log.
     */
    @Override
    public void i(String msg) {
        Log.i(TAG, formatMsg(msg));
    }

    /**
     * Logs an info-level message with an exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    @Override
    public void i(String msg, Throwable t) {
        Log.i(TAG, formatMsg(msg), t);
    }

    /**
     * Logs a warning-level message.
     *
     * @param msg The message to log.
     */
    @Override
    public void w(String msg) {
        Log.w(TAG, formatMsg(msg));
    }

    /**
     * Logs a warning-level message with an exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    @Override
    public void w(String msg, Throwable t) {
        Log.w(TAG, formatMsg(msg), t);
    }

    @Override
    public void d(String msg) {
        Log.d(TAG, formatMsg(msg));
    }

    /**
     * Logs a debug-level message.
     *
     * @param msg The message to log.
     */
    @Override
    public void d(String msg, Throwable t) {
        Log.d(TAG, formatMsg(msg), t);
    }
}
