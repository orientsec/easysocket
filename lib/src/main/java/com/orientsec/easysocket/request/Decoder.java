package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

/**
 * A generic interface for decoding packets into specific types.
 *
 * @param <T> The type of the object that the packet will be decoded into.
 */
public interface Decoder<T> {

    /**
     * Decodes the given packet into an object of type T.
     *
     * @param packet The packet to be decoded.
     * @return The decoded object of type T.
     */
    @NonNull
    T decode(@NonNull Packet packet);
}