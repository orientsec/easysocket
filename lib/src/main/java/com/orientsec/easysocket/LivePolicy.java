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
     * 弱连接策略。二进制位（0000 0000）
     * 1.活动无自动重连；
     * 2.休眠无自动重连。
     */
    WEAK((byte) 4),

    /**
     * 默认策略。二进制位（0000 0001）
     * 1.活动自动重连；
     * 2.休眠无自动重连。
     */
    DEFAULT((byte) 1),

    /**
     * 强连接策略。二进制位（0000 0011）
     * 1.活动自动重连；
     * 2.休眠自动重连。
     */
    STRONG((byte) 3);

    //活动时自动连接
    private static final byte MASK_AUTO_CONNECT_LIVE = 1;
    //休眠时自动连接
    private static final byte MASK_AUTO_CONNECT_SLEEP = 2;

    /**
     * 连接标识位，使用最后3位做标识。
     * 第一位标识活动时是否自动重连；
     * 第二位标识休眠时是否自动重连；
     */
    final byte flag;

    LivePolicy(byte flag) {
        this.flag = flag;
    }

    /**
     * 是否自动连接
     *
     * @param active 应用是否活跃。
     * @return 是否自动连接。
     */
    public boolean autoConnect(boolean active) {
        byte mask;
        if (active) {
            mask = MASK_AUTO_CONNECT_LIVE;
        } else {
            mask = MASK_AUTO_CONNECT_SLEEP;
        }
        return (mask & flag) == mask;
    }

}
