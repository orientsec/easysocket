package com.orientsec.easysocket.push

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.SocketClient
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Abstract implementation of the PushManager interface, providing common functionality
 * for managing push listeners and handling packets in the EasySocket library.
 *
 * @param <K> The type of the key used to identify push listeners.
 * @param <E> The type of the entity parsed from a packet.
 */
abstract class AbstractPushManager<K, E>(client: SocketClient) : PushManager<K, E> {
    // Logger instance for logging messages
    private val logger: Logger = client.logger

    private val scope = client.scope

    private val options = client.options

    // Mutex for protecting listener maps and sets
    private val mutex = Mutex()

    // Map to store push listeners associated with specific keys
    private val idListenerMap = mutableMapOf<K, MutableSet<PushListener<E>>>()

    // Set to store global push listeners not associated with specific keys
    private val globalListenerSet = mutableSetOf<PushListener<E>>()

    /**
     * Registers a push listener associated with a specific key.
     */
    override fun registerPushListener(key: K, pushListener: PushListener<E>) {
        scope.launch {
            mutex.withLock {
                val listeners = idListenerMap.getOrPut(key) { mutableSetOf() }
                listeners.add(pushListener)
            }
        }
    }

    /**
     * Unregisters a push listener associated with a specific key.
     */
    override fun unregisterPushListener(key: K, pushListener: PushListener<E>) {
        scope.launch {
            mutex.withLock {
                idListenerMap[key]?.remove(pushListener)
            }
        }
    }

    /**
     * Registers a global push listener that is not associated with a specific key.
     */
    override fun registerPushListener(pushListener: PushListener<E>) {
        scope.launch {
            mutex.withLock {
                globalListenerSet.add(pushListener)
            }
        }
    }

    /**
     * Unregisters a global push listener that is not associated with a specific key.
     */
    override fun unregisterPushListener(pushListener: PushListener<E>) {
        scope.launch {
            mutex.withLock {
                globalListenerSet.remove(pushListener)
            }
        }
    }

    /**
     * Handles an incoming packet by parsing it and dispatching the resulting event.
     */
    override fun handlePacket(packet: Packet) {
        scope.launch(options.codecDispatcher) {
            parsePacket(packet)
                .onSuccess {
                    val key = eventKey(packet, it)
                    withContext(options.callbackDispatcher) {
                        sendPushEvent(key, it)
                    }
                }.onFailure {
                    withContext(options.callbackDispatcher) {
                        onError(it)
                    }
                }
        }
    }

    protected abstract fun onError(e: Throwable)

    protected abstract fun eventKey(packet: Packet, event: E): K

    /**
     * Dispatches a push event to the appropriate listeners based on the key.
     */
    protected open suspend fun sendPushEvent(key: K, event: E) {
        val listenersToNotify = mutex.withLock {
            val listeners = idListenerMap[key]?.toList() ?: emptyList()
            val globals = globalListenerSet.toList()
            listeners to globals
        }

        if (listenersToNotify.first.isEmpty()) {
            logger.w("no push listener registered for event: $key")
        } else {
            for (listener in listenersToNotify.first) {
                listener.onPush(event)
            }
        }

        for (listener in listenersToNotify.second) {
            listener.onPush(event)
        }
    }
}
