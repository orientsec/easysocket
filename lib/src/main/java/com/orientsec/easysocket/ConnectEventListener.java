package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 13:50
 * Author: Fredric
 * coding is art not science
 * <p>
 * 连接事件监听
 */
public interface ConnectEventListener {
    /**
     * 连接断开后进行的回调
     */
    void onDisconnect();

    /**
     * 当连接连接建立成功后
     */
    void onConnect();

    /**
     * 当连接失败时会进行回调
     * 如果服务器出现故障,网络出现异常都将导致该方法被回调
     */
    void onConnectFailed();
}
