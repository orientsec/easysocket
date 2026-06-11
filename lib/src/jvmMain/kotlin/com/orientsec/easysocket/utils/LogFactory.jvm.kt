package com.orientsec.easysocket.utils

import com.orientsec.easysocket.Options

/**
 * JVM 平台的 [LogFactory] 实现。
 * 当前为占位实现，尚未完成具体功能。
 */
actual object LogFactory {
    /**
     * 创建日志记录器，尚未实现。
     */
    actual fun getLogger(
        options: Options,
        suffix: String
    ): Logger {
        TODO("Not yet implemented")
    }
}