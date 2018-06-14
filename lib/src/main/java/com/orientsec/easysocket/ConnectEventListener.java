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
     *
     * @param error 0 正常断开
     *              1 IO异常
     *              2 网络不可用
     *              3 心跳检测
     *              4 睡眠
     *              999 未知异常
     *              -1 消息读取出错
     *              -2 授权失败
     *
     */
    void onDisconnect(int error);

    /**
     * 连接连接建立成功后的回调
     */
    void onConnect();

    /**
     * 当连接失败时会进行回调
     * 如果服务器出现故障,网络出现异常都将导致该方法被回调
     */
    void onConnectFailed();
}
