package com.orientsec.easysocket.utils

import kotlinx.coroutines.CoroutineDispatcher

/**
 * JVM 平台的 [Platform] 实现。
 * 当前为占位实现，尚未完成具体功能。
 */
actual object Platform {
    /** 主线程调度器，尚未实现 */
    actual val mainDispatcher: CoroutineDispatcher
        get() = TODO("Not yet implemented")

    /** 获取当前时间，尚未实现 */
    actual fun currentTimeMillis(): Long {
        TODO("Not yet implemented")
    }

    /** 日志输出，空实现 */
    actual fun log(
        level: Int,
        tag: String,
        msg: String,
        throwable: Throwable?
    ) {
    }

    /** JVM 平台的日志级别常量，尚未实现 */
    actual object LogLevel {
        actual const val DEBUG: Int
            get() = TODO("Not yet implemented")
        actual const val INFO: Int
            get() = TODO("Not yet implemented")
        actual const val WARN: Int
            get() = TODO("Not yet implemented")
        actual const val ERROR: Int
            get() = TODO("Not yet implemented")
    }
}