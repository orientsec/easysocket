package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.error.EasyException;

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
    void onDisconnect(@NonNull EasyException e);

    /**
     * 连接连接建立成功后的回调
     */
    void onConnect();

    /**
     * 当连接失败时会进行回调
     * 如果服务器出现故障,网络出现异常都将导致该方法被回调
     */
    void onConnectFailed();

    /**
     * 登入服务器回调，所有请求在登入成功之后才能发起。
     * 客户端接入服务器后，有可能会被主动断开，需要通过login事件确认是否成功接入。
     */
    void onAvailable();
}
