package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2018/01/18 09:08
 * Author: Fredric
 * coding is art not science
 */
public enum LivePolicy {
    /**
     * 默认策略
     * 1.前台保活，如果连接断开会自动重连
     * 2.退到后台超过后台运行时间，自动断开
     */
    DEFAULT,
    /**
     * 强连接策略
     * 1.应用运行期间，连接断开后自动重连
     * 2.应用退到后台不会断开连接
     */
    STRONG
}
