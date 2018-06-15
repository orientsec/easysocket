package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 14:05
 * Author: Fredric
 * coding is art not science
 * <p>
 * 推送消息监听
 */
public interface PushHandler<T> {
    /**
     * 推送消息回调
     *
     * @param message 消息体
     */
    void onPush(T message);
}
