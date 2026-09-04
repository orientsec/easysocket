package com.orientsec.easysocket.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.locks.ReentrantLock

/**
 * JVM 平台的 [Platform] 实现。
 */
actual object Platform {
    /**
     * JVM "主线程"调度器。
     *
     * 存在 UI 环境（Swing/JavaFX，或引入对应的 kotlinx-coroutines 模块）时
     * 使用 Dispatchers.Main；headless JVM（纯后端/桌面无 GUI）没有主线程概念，
     * 回退到 Dispatchers.Default。
     *
     * 注意：桌面应用若需回调在 UI 线程执行，应显式设置
     * Options.Builder.callbackDispatcher（如 Compose Desktop 传入 Swing 调度器）。
     */
    actual val mainDispatcher: CoroutineDispatcher by lazy {
        try {
            Dispatchers.Main
        } catch (_: IllegalStateException) {
            // 无 Main dispatcher 工厂（未引入 coroutines-swing/javafx 等）
            Dispatchers.Default
        }
    }

    /** JVM IO 调度器 */
    actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /** 获取当前时间 */
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    /** 日志输出 */
    actual fun log(
        level: Int,
        tag: String,
        msg: String,
        throwable: Throwable?
    ) {
        val levelStr = when (level) {
            LogLevel.DEBUG -> "DEBUG"
            LogLevel.INFO -> "INFO"
            LogLevel.WARN -> "WARN"
            LogLevel.ERROR -> "ERROR"
            else -> "UNKNOWN"
        }
        println("[$levelStr][$tag] $msg")
        throwable?.printStackTrace()
    }

    /** JVM 平台的日志级别常量 */
    actual object LogLevel {
        actual val DEBUG = 3
        actual val INFO = 4
        actual val WARN = 5
        actual val ERROR = 6
    }

    actual fun createSingleThreadDispatcher(name: String): CoroutineDispatcher {
        val factory = ThreadFactory {
            Thread(it, name)
        }
        return Executors.newSingleThreadExecutor(factory).asCoroutineDispatcher()
    }

    actual interface Lock {
        actual fun lock()
        actual fun unlock()
    }

    actual fun createLock(): Lock = object : Lock {
        private val lock = ReentrantLock()
        override fun lock() = lock.lock()
        override fun unlock() = lock.unlock()
    }
}
