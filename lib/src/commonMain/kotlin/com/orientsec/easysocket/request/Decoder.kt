package com.orientsec.easysocket.request

import com.orientsec.easysocket.Packet

/**
 * A generic interface for decoding packets into specific types.
 *
 * @param <T> The type of the object that the packet will be decoded into.
 */
interface Decoder<T> {

    /**
     * Decodes the given packet into an object of type T.
     *
     * @param packet The packet to be decoded.
     * @return The decoded object of type T.
     */
    fun decode(packet: Packet): T
}
