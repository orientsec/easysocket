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
public interface PushHandler {
    /**
     * 推送消息
     *
     * @param id      消息id
     * @param message 消息体
     */
    void onPush(int id, Message message);
}
