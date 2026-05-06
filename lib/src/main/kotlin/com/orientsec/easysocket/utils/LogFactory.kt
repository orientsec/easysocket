package com.orientsec.easysocket.utils

import com.orientsec.easysocket.Options

/**
 * Factory class for creating logger instances.
 */
object LogFactory {

    /**
     * Returns a logger instance based on the provided options.
     *
     * @param options The configuration options for logging.
     * @param suffix  A suffix to append to the logger name.
     * @return A logger instance. If debugging is enabled, an AndroidLogger is returned;
     * otherwise, a NoLogger is returned.
     */
    @JvmStatic
    fun getLogger(options: Options, suffix: String): Logger {
        return if (options.isDebuggable) {
            AndroidLogger(suffix, options.minLogLevel)
        } else {
            NoLogger()
        }
    }
}
