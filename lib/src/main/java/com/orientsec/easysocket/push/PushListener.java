package com.orientsec.easysocket.push;

/**
 * A listener interface for receiving push notifications.
 * Implementations of this interface should define how to handle
 * the received push data of type T.
 *
 * @param <T> The type of the data received in the push notification.
 */
public interface PushListener<T> {

    /**
     * Called when a push notification is received.
     *
     * @param t The data received in the push notification.
     */
    void onPush(T t);
}