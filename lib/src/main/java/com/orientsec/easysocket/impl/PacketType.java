package com.orientsec.easysocket.impl;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 10:44
 * Author: Fredric
 * coding is art not science
 */

public enum PacketType {
    /**
     * 请求消息
     */
    RESPONSE("OUT"),

    /**
     * 推送消息
     */
    PUSH("push"),

    /**
     * 心跳
     */
    PULSE("pulse");

    private String value;

    PacketType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
