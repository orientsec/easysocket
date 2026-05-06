package com.orientsec.easysocket.push

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.SocketClient
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor

/**
 * Abstract implementation of the PushManager interface, providing common functionality
 * for managing push listeners and handling packets in the EasySocket library.
 *
 * @param <K> The type of the key used to identify push listeners.
 * @param <E> The type of the entity parsed from a packet.
 */
abstract class AbstractPushManager<K, E>(private val client: SocketClient) : PushManager<K, E> {
    // Logger instance for logging messages
    private val logger: Logger = client.logger

    // Executor for handling legacy callback tasks
    private val callbackExecutor: Executor = client.options.callbackExecutor

    // Map to store push listeners associated with specific keys
    private val idListenerMap = ConcurrentHashMap<K, CopyOnWriteArraySet<PushListener<E>>>()

    // Set to store global push listeners not associated with specific keys
    private val globalListenerSet = CopyOnWriteArraySet<PushListener<E>>()

    /**
     * Registers a push listener associated with a specific key.
     *
     * @param key          The key used to identify the push listener.
     * @param pushListener The push listener to be registered.
     */
    override fun registerPushListener(key: K, pushListener: PushListener<E>) {
        idListenerMap.computeIfAbsent(key) { CopyOnWriteArraySet() }.add(pushListener)
    }

    /**
     * Unregisters a push listener associated with a specific key.
     *
     * @param key          The key used to identify the push listener.
     * @param pushListener The push listener to be unregistered.
     */
    override fun unregisterPushListener(key: K, pushListener: PushListener<E>) {
        idListenerMap[key]?.remove(pushListener)
    }

    /**
     * Registers a global push listener that is not associated with a specific key.
     *
     * @param pushListener The push listener to be registered.
     */
    override fun registerPushListener(pushListener: PushListener<E>) {
        globalListenerSet.add(pushListener)
    }

    /**
     * Unregisters a global push listener that is not associated with a specific key.
     *
     * @param pushListener The push listener to be unregistered.
     */
    override fun unregisterPushListener(pushListener: PushListener<E>) {
        globalListenerSet.remove(pushListener)
    }

    /**
     * Handles an incoming packet by parsing it and dispatching the resulting event
     * to the appropriate push listeners.
     *
     * @param packet The packet to be handled.
     */
    override fun handlePacket(packet: Packet) {
        client.scope.launch {
            withContext(Dispatchers.Default) {
                parsePacket(packet)
            }.onSuccess {
                val key = eventKey(packet, it)
                callbackExecutor.execute { sendPushEvent(key, it) }
            }.onFailure {
                onError(it)
            }
        }
    }

    /**
     * Called when an error occurs during packet handling or event dispatching.
     *
     * @param e The exception that occurred.
     */
    protected abstract fun onError(e: Throwable)

    /**
     * Extracts the key associated with an event from the given packet and event.
     *
     * @param packet The packet containing the event data.
     * @param event  The parsed event.
     * @return The key associated with the event.
     */
    protected abstract fun eventKey(packet: Packet, event: E): K

    /**
     * Dispatches a push event to the appropriate listeners based on the key.
     *
     * @param key   The key associated with the event.
     * @param event The event to be dispatched.
     */
    protected open fun sendPushEvent(key: K, event: E) {
        val set = idListenerMap[key]
        if (!set.isNullOrEmpty()) {
            for (listener in set) {
                listener.onPush(event)
            }
        } else {
            logger.w("no push listener registered for event: $key")
        }
        
        for (listener in globalListenerSet) {
            listener.onPush(event)
        }
    }
}
