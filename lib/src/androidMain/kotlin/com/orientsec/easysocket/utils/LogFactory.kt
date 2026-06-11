package com.orientsec.easysocket.utils

import com.orientsec.easysocket.Options

/**
 * Android 平台的 [LogFactory] 实现。
 *
 * 根据配置决定创建 [AndroidLogger] 或 [NoLogger]：
 * - 调试模式（isDebuggable = true）下创建 [AndroidLogger]，输出到 Android Log
 * - 非调试模式下创建 [NoLogger]，不输出任何日志
 */
actual object LogFactory {
    /**
     * 根据配置创建日志记录器。
     *
     * @param options 配置选项
     * @param suffix 日志后缀
     * @return 日志记录器实例
     */
    actual fun getLogger(options: Options, suffix: String): Logger {
        return if (options.isDebuggable) {
            AndroidLogger(suffix, options.minLogLevel)
        } else {
            NoLogger()
        }
    }
}