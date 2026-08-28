package com.orientsec.easysocket.utils

/**
 * iOS 平台的日志实现，直接使用 [Platform.log]。
 */
class IosLogger(private val name: String, private val suffix: String) : Logger {
    private val tag = if (suffix.isEmpty()) name else "$name[$suffix]"

    override fun d(msg: String) {
        Platform.log(Platform.LogLevel.DEBUG, tag, msg)
    }

    override fun d(msg: String, t: Throwable?) {
        Platform.log(Platform.LogLevel.DEBUG, tag, msg, t)
    }

    override fun i(msg: String) {
        Platform.log(Platform.LogLevel.INFO, tag, msg)
    }

    override fun i(msg: String, t: Throwable?) {
        Platform.log(Platform.LogLevel.INFO, tag, msg, t)
    }

    override fun w(msg: String) {
        Platform.log(Platform.LogLevel.WARN, tag, msg)
    }

    override fun w(msg: String, t: Throwable?) {
        Platform.log(Platform.LogLevel.WARN, tag, msg, t)
    }

    override fun e(msg: String) {
        Platform.log(Platform.LogLevel.ERROR, tag, msg)
    }

    override fun e(msg: String, t: Throwable?) {
        Platform.log(Platform.LogLevel.ERROR, tag, msg, t)
    }
}
