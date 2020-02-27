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

public abstract class AbstractPushManager<T, E> implements PushManager<T, E> {
    private Executor codecExecutor;
    private Executor callbackExecutor;

    private Map<String, Set<PushListener<E>>> listenerMap = new HashMap<>();

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
            (String key, PushListener<E> pushListener) {
        Set<PushListener<E>> set = listenerMap.get(key);
        if (set == null) {
            set = new HashSet<>();
            listenerMap.put(key, set);
        }
        set.add(pushListener);
    }

    @Override
    public synchronized void unregisterPushListener
            (String key, PushListener<E> pushListener) {
        Set<PushListener<E>> set = listenerMap.get(key);
        if (set != null) {
            set.remove(pushListener);
        }
    }

    @Override
    public void handlePacket(Packet<T> packet) {
        codecExecutor.execute(() -> {
            try {
                E event = parsePacket(packet);
                String key = eventKey(event);
                callbackExecutor.execute(() -> sendPushEvent(key, event));
            } catch (EasyException e) {
                onError(e);
            }
        });
    }

    abstract void onError(EasyException e);

    abstract String eventKey(E event);

    protected synchronized void sendPushEvent(String key, E event) {
        Set<PushListener<E>> set = listenerMap.get(key);
        if (set != null && !set.isEmpty()) {
            for (PushListener<E> listener : set) {
                listener.onPush(event);
            }
        } else {
            Logger.w("No push lister for event: " + key);
        }
    }
}
