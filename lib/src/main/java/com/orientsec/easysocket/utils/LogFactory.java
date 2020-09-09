package com.orientsec.easysocket.utils;

import com.orientsec.easysocket.BuildConfig;

public class LogFactory {
    public static Logger getLogger(String name) {
        return getLogger(name, BuildConfig.DEBUG);
    }

    public static Logger getLogger(String name, boolean debuggable) {
        if (debuggable) {
            return new AndroidLogger(name);
        } else {
            return new NoLogger();
        }
    }
}
