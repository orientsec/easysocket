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

public abstract class AbstractPushManager<K, E> implements PushManager<K, E> {
    private final Logger logger;
    private final Executor codecExecutor;
    private final Executor callbackExecutor;

    private final Map<K, Set<PushListener<E>>> idListenerMap = new HashMap<>();

    private final Set<PushListener<E>> globalListenerSet = new HashSet<>();

    public AbstractPushManager(SocketClient client) {
        logger = client.getLogger();
        Options options = client.getOptions();
        codecExecutor = options.getCodecExecutor();
        callbackExecutor = options.getCallbackExecutor();
    }

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

    @Override
    public synchronized void unregisterPushListener
            (@NonNull K key, @NonNull PushListener<E> pushListener) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set != null) {
            set.remove(pushListener);
        }
    }

    @Override
    public void registerPushLister(@NonNull PushListener<E> pushListener) {
        globalListenerSet.add(pushListener);
    }

    @Override
    public void unregisterPushListener(@NonNull PushListener<E> pushListener) {
        globalListenerSet.remove(pushListener);
    }

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

    protected abstract void onError(Exception e);

    protected abstract K eventKey(@NonNull Packet packet, E event);

    protected synchronized void sendPushEvent(K key, E event) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set != null && !set.isEmpty()) {
            for (PushListener<E> listener : set) {
                listener.onPush(event);
            }
        } else {
            logger.w("No push lister registered for event: " + key);
        }
        if (!globalListenerSet.isEmpty()) {
            for (PushListener<E> listener : globalListenerSet) {
                listener.onPush(event);
            }
        }
    }
}
