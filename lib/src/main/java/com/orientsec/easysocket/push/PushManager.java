package com.orientsec.easysocket.push;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.exception.EasyException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 14:05
 * Author: Fredric
 * coding is art not science
 * <p>
 * 推送消息监听
 */
public interface PushManager<K, E> extends PacketHandler {

    E parsePacket(Packet<?> packet) throws EasyException;

    void registerPushListener(K key, PushListener<E> pushListener);

    void registerPushLister(PushListener<E> pushListener);

    void unregisterPushListener(K key, PushListener<E> pushListener);

    void unregisterPushListener(PushListener<E> pushListener);

}
