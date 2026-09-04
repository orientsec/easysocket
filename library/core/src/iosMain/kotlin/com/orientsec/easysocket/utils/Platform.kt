package com.orientsec.easysocket.utils

import kotlinx.coroutines.*
import platform.Foundation.NSDate
import platform.Foundation.NSLock
import platform.Foundation.timeIntervalSince1970

/**
 * iOS 平台的 [Platform] 实现。
 */
actual object Platform {
    /** iOS 主线程调度器 */
    actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    /** iOS IO 调度器 */
    actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

    /** 获取当前系统时间（毫秒） */
    actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    /**
     * iOS 平台日志输出。
     *
     * @param level 日志级别
     * @param tag 日志标签
     * @param msg 日志消息
     * @param throwable 可选的异常信息
     */
    actual fun log(level: Int, tag: String, msg: String, throwable: Throwable?) {
        val levelStr = when (level) {
            LogLevel.DEBUG -> "DEBUG"
            LogLevel.INFO -> "INFO"
            LogLevel.WARN -> "WARN"
            LogLevel.ERROR -> "ERROR"
            else -> "UNKNOWN"
        }
        val output = "[$levelStr][$tag] $msg"
        println(output)
        throwable?.printStackTrace()
    }

    /** iOS 平台的日志级别常量 */
    actual object LogLevel {
        actual const val DEBUG = 3
        actual const val INFO = 4
        actual const val WARN = 5
        actual const val ERROR = 6
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    actual fun createSingleThreadDispatcher(name: String): CoroutineDispatcher {
        return newSingleThreadContext(name)
    }

    actual interface Lock {
        actual fun lock()
        actual fun unlock()
    }

    actual fun createLock(): Lock = object : Lock {
        private val nsLock = NSLock()
        override fun lock() = nsLock.lock()
        override fun unlock() = nsLock.unlock()
    }
}
