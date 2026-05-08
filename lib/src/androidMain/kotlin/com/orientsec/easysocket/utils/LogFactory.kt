package com.orientsec.easysocket.utils

import com.orientsec.easysocket.Options

actual object LogFactory {
    actual fun getLogger(options: Options, suffix: String): Logger {
        return if (options.isDebuggable) {
            AndroidLogger(suffix, options.minLogLevel)
        } else {
            NoLogger()
        }
    }
}
