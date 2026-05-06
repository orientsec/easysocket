package com.orientsec.easysocket.utils

/**
 * Logger is an interface for logging messages at various levels (error, info, warning, debug).
 * It provides methods to log messages with or without associated exceptions.
 */
interface Logger {

    /**
     * Logs an error-level message.
     *
     * @param msg The message to log.
     */
    fun e(msg: String)

    /**
     * Logs an error-level message with an associated exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    fun e(msg: String, t: Throwable?)

    /**
     * Logs an info-level message.
     *
     * @param msg The message to log.
     */
    fun i(msg: String)

    /**
     * Logs an info-level message with an associated exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    fun i(msg: String, t: Throwable?)

    /**
     * Logs a warning-level message.
     *
     * @param msg The message to log.
     */
    fun w(msg: String)

    /**
     * Logs a warning-level message with an associated exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    fun w(msg: String, t: Throwable?)

    /**
     * Logs a debug-level message.
     *
     * @param msg The message to log.
     */
    fun d(msg: String)

    /**
     * Logs a debug-level message.
     *
     * @param msg The message to log.
     */
    fun d(msg: String, t: Throwable?)
}
