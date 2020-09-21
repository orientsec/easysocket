package com.orientsec.easysocket.utils;

import com.orientsec.easysocket.Options;

public class LogFactory {

    public static Logger getLogger(Options options) {
        if (options.isDebug()) {
            return new AndroidLogger(options);
        } else {
            return new NoLogger();
        }
    }
}
