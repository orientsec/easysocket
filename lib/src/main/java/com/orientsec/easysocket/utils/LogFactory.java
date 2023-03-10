package com.orientsec.easysocket.utils;

import com.orientsec.easysocket.Options;

public class LogFactory {

    public static Logger getLogger(Options options, String suffix) {
        if (options.isDebug()) {
            return new AndroidLogger(options, suffix);
        } else {
            return new NoLogger();
        }
    }
}
