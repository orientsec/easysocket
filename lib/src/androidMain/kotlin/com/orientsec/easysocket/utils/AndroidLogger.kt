package com.orientsec.easysocket.utils

import android.util.Log

/**
 * Android 平台的日志记录器实现。
 *
 * 使用 [android.util.Log] 输出日志，支持日志级别过滤和详细格式化。
 * 日志消息会附加后缀和当前线程名称，便于调试追踪。
 *
 * @param suffix 日志后缀，包含会话和客户端标识信息
 * @param minLogLevel 最低日志输出级别，低于此级别的日志将被忽略
 */
internal class AndroidLogger(
    private val suffix: String,
    private val minLogLevel: Int
) : Logger {

    /**
     * 格式化日志消息，附加后缀和当前线程名称。
     *
     * @param msg 原始日志消息
     * @return 格式化后的消息
     */
    private fun formatMsg(msg: String): String {
        return msg + suffix + "  Thread:[" + Thread.currentThread().name + "]"
    }

    /**
     * 检查指定日志级别是否应被忽略。
     *
     * @param level 日志级别
     * @return true 如果应忽略
     */
    private fun isIgnored(level: Int): Boolean {
        return level < minLogLevel
    }

    /** 输出 ERROR 级别日志 */
    override fun e(msg: String) {
        if (isIgnored(Log.ERROR)) return
        Log.e(TAG, formatMsg(msg))
    }

    /** 输出 ERROR 级别日志，附带异常信息 */
    override fun e(msg: String, t: Throwable?) {
        if (isIgnored(Log.ERROR)) return
        Log.e(TAG, formatMsg(msg), t)
    }

    /** 输出 INFO 级别日志 */
    override fun i(msg: String) {
        if (isIgnored(Log.INFO)) return
        Log.i(TAG, formatMsg(msg))
    }

    /** 输出 INFO 级别日志，附带异常信息 */
    override fun i(msg: String, t: Throwable?) {
        if (isIgnored(Log.INFO)) return
        Log.i(TAG, formatMsg(msg), t)
    }

    /** 输出 WARN 级别日志 */
    override fun w(msg: String) {
        if (isIgnored(Log.WARN)) return
        Log.w(TAG, formatMsg(msg))
    }

    /** 输出 WARN 级别日志，附带异常信息 */
    override fun w(msg: String, t: Throwable?) {
        if (isIgnored(Log.WARN)) return
        Log.w(TAG, formatMsg(msg), t)
    }

    /** 输出 DEBUG 级别日志 */
    override fun d(msg: String) {
        if (isIgnored(Log.DEBUG)) return
        Log.d(TAG, formatMsg(msg))
    }

    /** 输出 DEBUG 级别日志，附带异常信息 */
    override fun d(msg: String, t: Throwable?) {
        if (isIgnored(Log.DEBUG)) return
        Log.d(TAG, formatMsg(msg), t)
    }

    companion object {
        /** 默认日志标签 */
        private const val TAG = "EasySocket"
    }
}