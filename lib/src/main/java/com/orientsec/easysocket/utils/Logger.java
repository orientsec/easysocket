package com.orientsec.easysocket.utils;

/**
 * Logger is an interface for logging messages at various levels (error, info, warning, debug).
 * It provides methods to log messages with or without associated exceptions.
 */
public interface Logger {

    /**
     * Logs an error-level message.
     *
     * @param msg The message to log.
     */
    void e(String msg);

    /**
     * Logs an error-level message with an associated exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    void e(String msg, Throwable t);

    /**
     * Logs an info-level message.
     *
     * @param msg The message to log.
     */
    void i(String msg);

    /**
     * Logs an info-level message with an associated exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    void i(String msg, Throwable t);

    /**
     * Logs a warning-level message.
     *
     * @param msg The message to log.
     */
    void w(String msg);

    /**
     * Logs a warning-level message with an associated exception.
     *
     * @param msg The message to log.
     * @param t   The exception to include in the log.
     */
    void w(String msg, Throwable t);

    /**
     * Logs a debug-level message.
     *
     * @param msg The message to log.
     */
    void d(String msg);

    /**
     * Logs a debug-level message.
     *
     * @param msg The message to log.
     */
    void d(String msg, Throwable t);
}