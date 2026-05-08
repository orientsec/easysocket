package com.orientsec.easysocket.utils

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-specific utilities for the EasySocket library.
 */
expect object Platform {
    /**
     * Retrieves the main thread dispatcher for the current platform.
     */
    val mainDispatcher: CoroutineDispatcher

    /**
     * Gets the current system time in milliseconds.
     */
    fun currentTimeMillis(): Long

    /**
     * Logs a message with the specified level and tag.
     */
    fun log(level: Int, tag: String, msg: String, throwable: Throwable? = null)

    /**
     * Constants for log levels, mirroring android.util.Log for compatibility.
     */
    object LogLevel {
        val DEBUG: Int
        val INFO: Int
        val WARN: Int
        val ERROR: Int
    }
}
