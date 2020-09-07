package com.orientsec.easysocket.push;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.EasyException;

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

    @NonNull
    E parsePacket(Packet packet) throws EasyException;

    void registerPushListener(@NonNull K key, @NonNull PushListener<E> pushListener);

    void registerPushLister(@NonNull PushListener<E> pushListener);

    void unregisterPushListener(@NonNull K key, @NonNull PushListener<E> pushListener);

    void unregisterPushListener(@NonNull PushListener<E> pushListener);

}
