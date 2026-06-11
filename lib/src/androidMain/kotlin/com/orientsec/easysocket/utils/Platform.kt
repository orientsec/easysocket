package com.orientsec.easysocket.utils

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Android 平台的 [Platform] 实现。
 *
 * 提供 Android 特定的功能：
 * - 主线程调度器使用 [Dispatchers.Main]
 * - 时间获取使用 [System.currentTimeMillis]
 * - 日志输出使用 [android.util.Log]
 * - 日志级别常量映射到 [Log] 的常量
 */
actual object Platform {
    /** Android 主线程调度器 */
    actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    /** 获取当前系统时间（毫秒） */
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    /**
     * 使用 Android Log 输出日志。
     *
     * @param level 日志级别
     * @param tag 日志标签
     * @param msg 日志消息
     * @param throwable 可选的异常信息
     */
    actual fun log(level: Int, tag: String, msg: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, msg, throwable)
            LogLevel.INFO -> Log.i(tag, msg, throwable)
            LogLevel.WARN -> Log.w(tag, msg, throwable)
            LogLevel.ERROR -> Log.e(tag, msg, throwable)
        }
    }

    /** Android 平台的日志级别常量，映射到 [Log] 的常量 */
    actual object LogLevel {
        /** DEBUG 级别 */
        actual const val DEBUG = Log.DEBUG
        /** INFO 级别 */
        actual const val INFO = Log.INFO
        /** WARN 级别 */
        actual const val WARN = Log.WARN
        /** ERROR 级别 */
        actual const val ERROR = Log.ERROR
    }
}