package com.orientsec.easysocket.push

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.PacketHandler

/**
 * The PushManager interface defines methods for managing push notifications
 * and handling packets in the EasySocket library. It extends the PacketHandler
 * interface and provides functionality for parsing packets and registering or
 * unregistering push listeners.
 *
 * @param <K> The type of the key used to identify push listeners.
 * @param <E> The type of the entity parsed from a packet.
 */
interface PushManager<K, E> : PacketHandler {

    /**
     * Parses a packet and converts it into an entity of type E.
     *
     * @param packet The packet to be parsed.
     * @return The parsed entity of type E.
     * @throws Exception If an error occurs during packet parsing.
     */
    fun parsePacket(packet: Packet): Result<E>

    /**
     * Registers a push listener with a specific key.
     *
     * @param key          The key used to identify the push listener.
     * @param pushListener The push listener to be registered.
     */
    fun registerPushListener(key: K, pushListener: PushListener<E>)

    /**
     * Registers a push listener without a specific key.
     *
     * @param pushListener The push listener to be registered.
     */
    fun registerPushListener(pushListener: PushListener<E>)

    /**
     * Unregisters a push listener associated with a specific key.
     *
     * @param key          The key used to identify the push listener.
     * @param pushListener The push listener to be unregistered.
     */
    fun unregisterPushListener(key: K, pushListener: PushListener<E>)

    /**
     * Unregisters a push listener without a specific key.
     *
     * @param pushListener The push listener to be unregistered.
     */
    fun unregisterPushListener(pushListener: PushListener<E>)
}
