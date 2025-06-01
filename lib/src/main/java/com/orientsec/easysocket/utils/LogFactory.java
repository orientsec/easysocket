package com.orientsec.easysocket.utils;

import com.orientsec.easysocket.Options;

/**
 * Factory class for creating logger instances.
 */
public class LogFactory {

    /**
     * Returns a logger instance based on the provided options.
     *
     * @param options The configuration options for logging.
     * @param suffix  A suffix to append to the logger name.
     * @return A logger instance. If debugging is enabled, an AndroidLogger is returned;
     * otherwise, a NoLogger is returned.
     */
    public static Logger getLogger(Options options, String suffix) {
        if (options.isDebug()) {
            return new AndroidLogger(options, suffix);
        } else {
            return new NoLogger();
        }
    }
}