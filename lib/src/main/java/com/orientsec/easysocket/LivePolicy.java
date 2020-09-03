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
     * 弱连接策略。二进制位（0000 0100）
     * 1.前台无自动重连；
     * 2.后台无自动重连；
     * 3.进入休眠，自动断开连接。
     */
    WEAK((byte) 4),

    /**
     * 软连接策略。二进制位（0000 0101）
     * 1.前台自动重连；
     * 2.后台无自动重连；
     * 3.进入休眠，自动断开连接。
     */
    SOFT((byte) 5),

    /**
     * 默认策略。二进制位（0000 0001）
     * 1.前台自动重连；
     * 2.后台无自动重连；
     * 3.进入休眠，不自动断开连接。
     */
    DEFAULT((byte) 1),

    /**
     * 强连接策略。二进制位（0000 0011）
     * 1.前台自动重连；
     * 2.后台自动重连；
     * 3.进入休眠，不自动断开连接。
     */
    STRONG((byte) 3);

    //休眠自动断开连接
    private static final byte MASK_AUTO_DISCONNECT = 4;
    //活动时自动连接
    private static final byte MASK_AUTO_CONNECT_LIVE = 1;
    //休眠时自动连接
    private static final byte MASK_AUTO_CONNECT_SLEEP = 2;

    /**
     * 连接标识位，使用最后3位做标识。
     * 第一位标识前台是否自动重连；
     * 第二位标识后台是否自动重连；
     * 第三位标识休眠后是否自动断开连接。
     */
    final byte flag;

    LivePolicy(byte flag) {
        this.flag = flag;
    }

    /**
     * 是否自动连接
     *
     * @param sleep 应用是否进入休眠。
     * @return 是否自动连接。
     */
    public boolean autoConnect(boolean sleep) {
        byte mask;
        if (sleep) {
            mask = MASK_AUTO_CONNECT_SLEEP;
        } else {
            mask = MASK_AUTO_CONNECT_LIVE;
        }
        return (mask & flag) == mask;
    }

    /**
     * 是否自动断开连接
     *
     * @return 是否自动断开连接。
     */
    public boolean autoDisconnect() {
        return (MASK_AUTO_DISCONNECT & flag) == MASK_AUTO_DISCONNECT;
    }
}
