package com.orientsec.easysocket;

import androidx.annotation.NonNull;

/**
 * The `Provider` interface defines a generic contract for providing instances of a specific type.
 * Implementations of this interface are responsible for supplying objects, typically based on
 * the provided `SocketClient` instance.
 *
 * @param <T> The type of object that this provider supplies.
 */
public interface Provider<T> {

    /**
     * Retrieves an instance of the specified type.
     *
     * @param socketClient The `SocketClient` instance used to obtain the object.
     * @return An instance of type `T`.
     */
    @NonNull
    T get(SocketClient socketClient);
}