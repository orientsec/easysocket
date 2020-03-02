package com.orientsec.easysocket.push;

import com.orientsec.easysocket.Executors;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.utils.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public abstract class AbstractPushManager<T, K, E> implements PushManager<T, K, E> {
    private Executor codecExecutor;
    private Executor callbackExecutor;

    private Map<K, Set<PushListener<E>>> idListenerMap = new HashMap<>();

    private Set<PushListener<E>> globalListenerSet = new HashSet<>();

    public AbstractPushManager() {
        codecExecutor = Executors.codecExecutor;
        callbackExecutor = Executors.mainThreadExecutor;
    }

    public AbstractPushManager(Executor codecExecutor, Executor callbackExecutor) {
        this.codecExecutor = codecExecutor;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public synchronized void registerPushListener
            (K key, PushListener<E> pushListener) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set == null) {
            set = new HashSet<>();
            idListenerMap.put(key, set);
        }
        set.add(pushListener);
    }

    @Override
    public synchronized void unregisterPushListener
            (K key, PushListener<E> pushListener) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set != null) {
            set.remove(pushListener);
        }
    }

    @Override
    public void registerPushLister(PushListener<E> pushListener) {
        globalListenerSet.add(pushListener);
    }

    @Override
    public void unregisterPushListener(PushListener<E> pushListener) {
        globalListenerSet.remove(pushListener);
    }

    @Override
    public void handlePacket(Packet<T> packet) {
        codecExecutor.execute(() -> {
            try {
                E event = parsePacket(packet);
                K key = eventKey(packet.getBody(), event);
                callbackExecutor.execute(() -> sendPushEvent(key, event));
            } catch (EasyException e) {
                onError(e);
            }
        });
    }

    protected abstract void onError(EasyException e);

    protected abstract K eventKey(T body, E event);

    protected synchronized void sendPushEvent(K key, E event) {
        Set<PushListener<E>> set = idListenerMap.get(key);
        if (set != null && !set.isEmpty()) {
            for (PushListener<E> listener : set) {
                listener.onPush(event);
            }
        } else {
            Logger.w("No push lister registered for event: " + key);
        }
        if (!globalListenerSet.isEmpty()) {
            for (PushListener<E> listener : globalListenerSet) {
                listener.onPush(event);
            }
        }
    }
}
