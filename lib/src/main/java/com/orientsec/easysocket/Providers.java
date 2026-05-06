package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import javax.net.SocketFactory;

/**
 * Provides the default implementation of a `Provider` for `SocketFactory`.
 * This class supplies the default `SocketFactory` instance.
 */
class DefaultSocketFactoryProvider implements Provider<SocketFactory> {

    /**
     * Retrieves the default `SocketFactory` instance.
     *
     * @param socketClient The `SocketClient` instance requesting the `SocketFactory`.
     * @return The default `SocketFactory` instance.
     */
    @NonNull
    @Override
    public SocketFactory get(SocketClient socketClient) {
        return SocketFactory.getDefault();
    }
}