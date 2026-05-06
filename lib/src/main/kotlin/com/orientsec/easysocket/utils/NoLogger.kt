package com.orientsec.easysocket.utils

/**
 * NoLogger is a no-op implementation of the Logger interface.
 * This class provides empty method implementations for all logging levels,
 * effectively disabling logging when used.
 */
internal class NoLogger : Logger {
    override fun e(msg: String) {}

    override fun e(msg: String, t: Throwable?) {}

    override fun i(msg: String) {}

    override fun i(msg: String, t: Throwable?) {}

    override fun w(msg: String) {}

    override fun w(msg: String, t: Throwable?) {}

    override fun d(msg: String) {}

    override fun d(msg: String, t: Throwable?) {}
}
