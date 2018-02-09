package com.orientsec.easysocket.inner;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 10:44
 * Author: Fredric
 * coding is art not science
 */

public enum MessageType {
    /**
     * 请求消息
     */
    REQUEST,
    /**
     * 推送消息
     */
    PUSH,
    /**
     * 心跳
     */
    PULSE,
    /**
     * 授权
     */
    AUTH
}
