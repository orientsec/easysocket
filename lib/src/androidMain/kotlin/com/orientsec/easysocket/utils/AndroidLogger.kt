package com.orientsec.easysocket.utils

import android.util.Log

/**
 * AndroidLogger is a utility class that implements the Logger interface for logging messages
 * on the Android platform. It provides methods to log messages at different levels (error,
 * info, warning, debug) and supports detailed logging with thread information and a custom suffix.
 */
internal class AndroidLogger(
    private val suffix: String,
    private val minLogLevel: Int
) : Logger {

    /**
     * Formats the log message by appending the suffix and thread information if detailed logging is enabled.
     *
     * @param msg The original log message.
     * @return The formatted log message.
     */
    private fun formatMsg(msg: String): String {
        return msg + suffix + "  Thread:[" + Thread.currentThread().name + "]"
    }

    /**
     * Checks if the log level is enabled for output.
     *
     * @param level The log level to check.
     * @return True if the log level is enabled, false otherwise.
     */
    private fun isIgnored(level: Int): Boolean {
        return level < minLogLevel
    }

    override fun e(msg: String) {
        if (isIgnored(Log.ERROR)) return
        Log.e(TAG, formatMsg(msg))
    }

    override fun e(msg: String, t: Throwable?) {
        if (isIgnored(Log.ERROR)) return
        Log.e(TAG, formatMsg(msg), t)
    }

    override fun i(msg: String) {
        if (isIgnored(Log.INFO)) return
        Log.i(TAG, formatMsg(msg))
    }

    override fun i(msg: String, t: Throwable?) {
        if (isIgnored(Log.INFO)) return
        Log.i(TAG, formatMsg(msg), t)
    }

    override fun w(msg: String) {
        if (isIgnored(Log.WARN)) return
        Log.w(TAG, formatMsg(msg))
    }

    override fun w(msg: String, t: Throwable?) {
        if (isIgnored(Log.WARN)) return
        Log.w(TAG, formatMsg(msg), t)
    }

    override fun d(msg: String) {
        if (isIgnored(Log.DEBUG)) return
        Log.d(TAG, formatMsg(msg))
    }

    override fun d(msg: String, t: Throwable?) {
        if (isIgnored(Log.DEBUG)) return
        Log.d(TAG, formatMsg(msg), t)
    }

    companion object {
        // The default tag used for logging
        private const val TAG = "EasySocket"
    }
}
