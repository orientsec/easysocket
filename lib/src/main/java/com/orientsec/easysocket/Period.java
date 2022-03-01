package com.orientsec.easysocket;

public enum Period {
    /**
     * dns解析。
     */
    DNS,
    /**
     * socket连接。
     */
    CONNECT,
    /**
     * ssl握手。
     */
    SSL,
    /**
     * 全阶段。
     */
    ALL
}
