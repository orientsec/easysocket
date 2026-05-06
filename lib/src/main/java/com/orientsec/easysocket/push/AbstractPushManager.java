package com.orientsec.easysocket.push;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.utils.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Abstract implementation of the PushManager interface, providing common functionality
 * for managing push listeners and handling packets in the EasySocket library.
 *
 * @param <K> The type of the key used to identify push listeners.
 * @param <E> The type of the entity parsed from a packet.
 */
public abstract class AbstractPushManager<K, E> implements PushManager<K, E> {
    // Logger instance for logging messages
    private final Logger logger;

    // Executor for handling codec-related tasks
    private final Executor codecExecutor;

    // Executor for handling callback-related tasks
    private final Executor callbackExecutor;

    // Map to store push listeners associated with specific keys
    private final Map<K, Set<PushListener<E>>> idListenerMap = new HashMap<>();

    // Set to store global push listeners not associated with specific keys
    private final Set<PushListener<E>> globalListenerSet = new HashSet<>();

    /**
     * Constructs an AbstractPushManager with the specified SocketClient.
     *
     * @param client The SocketClient instance used to initialize the manager.
     */
    public AbstractPushManager(SocketClient client) {
        logger = client.getLogger();
        Options options = client.getOptions();
        codecExecutor = options.getCodecExecutor();
        callbackExecutor = options.getCallbackExecutor();
    }

    /**
     * Registers a push listener associated with a specific key.
     *
     * @param key          The key used to identify the push listener.
     * @param pushListener The push listener to be registered.
     */
    @Override
    public synchronized void registerPushListener
    (@NonNull K key, @NonNull PushListener<E> pushListener) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set == null) {
            set = new HashSet<>();
            idListenerMap.put(key, set);
        }
        set.add(pushListener);
    }

    /**
     * Unregisters a push listener associated with a specific key.
     *
     * @param key          The key used to identify the push listener.
     * @param pushListener The push listener to be unregistered.
     */
    @Override
    public synchronized void unregisterPushListener
    (@NonNull K key, @NonNull PushListener<E> pushListener) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set != null) {
            set.remove(pushListener);
        }
    }

    /**
     * Registers a global push listener that is not associated with a specific key.
     *
     * @param pushListener The push listener to be registered.
     */
    @Override
    public void registerPushLister(@NonNull PushListener<E> pushListener) {
        globalListenerSet.add(pushListener);
    }

    /**
     * Unregisters a global push listener that is not associated with a specific key.
     *
     * @param pushListener The push listener to be unregistered.
     */
    @Override
    public void unregisterPushListener(@NonNull PushListener<E> pushListener) {
        globalListenerSet.remove(pushListener);
    }

    /**
     * Handles an incoming packet by parsing it and dispatching the resulting event
     * to the appropriate push listeners.
     *
     * @param packet The packet to be handled.
     */
    @Override
    public void handlePacket(@NonNull Packet packet) {
        codecExecutor.execute(() -> {
            try {
                E event = parsePacket(packet);
                K key = eventKey(packet, event);
                callbackExecutor.execute(() -> sendPushEvent(key, event));
            } catch (Exception e) {
                onError(e);
            }
        });
    }

    /**
     * Called when an error occurs during packet handling or event dispatching.
     *
     * @param e The exception that occurred.
     */
    protected abstract void onError(Exception e);

    /**
     * Extracts the key associated with an event from the given packet and event.
     *
     * @param packet The packet containing the event data.
     * @param event  The parsed event.
     * @return The key associated with the event.
     */
    protected abstract K eventKey(@NonNull Packet packet, E event);

    /**
     * Dispatches a push event to the appropriate listeners based on the key.
     *
     * @param key   The key associated with the event.
     * @param event The event to be dispatched.
     */
    protected synchronized void sendPushEvent(K key, E event) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set != null && !set.isEmpty()) {
            for (PushListener<E> listener : set) {
                listener.onPush(event);
            }
        } else {
            logger.w("no push lister registered for event: " + key);
        }
        if (!globalListenerSet.isEmpty()) {
            for (PushListener<E> listener : globalListenerSet) {
                listener.onPush(event);
            }
        }
    }
}