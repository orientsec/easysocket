package com.orientsec.easysocket.impl;


import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.Event;
import com.orientsec.easysocket.utils.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.annotations.NonNull;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 15:13
 * Author: Fredric
 * coding is art not science
 */
public abstract class AbstractConnection<T> implements Connection<T>,
        ConnectionManager.OnNetworkStateChangedListener, ConnectEventListener {
    /**
     * 0 空闲状态 0 -> 1
     * 1 连接中 1 -> 0; 1 -> 2
     * 2 连接成功 2 -> 3
     * 3 连接断开中 3 -> 0
     * 4 关闭 0 -> 4; 1 -> 4; 2 -> 4; 3 -> 4
     * <p>
     * 连接状态
     */
    AtomicInteger state = new AtomicInteger();

    private volatile long timestamp;

    volatile long connectTimestamp;

    private Set<ConnectEventListener> connectEventListeners
            = Collections.synchronizedSet(new HashSet<>());

    private ReConnector reConnector;

    ConnectionInfo connectionInfo;

    protected Options<T> options;

    private Executor callbackExecutor;

    private Executor managerExecutor;

    Pulse<T> pulse;

    public Options<T> options() {
        return options;
    }

    AbstractConnection(Options<T> options) {
        this.options = options;
        reConnector = new ReConnector<>(this);
        connectionInfo = options.getConnectionInfo();
        callbackExecutor = options.getCallbackExecutor();
        managerExecutor = options.getManagerExecutor();
    }

    @Override
    public boolean isShutdown() {
        return state.get() == 4;
    }

    @Override
    public void start() {
        if (state.compareAndSet(0, 1)) {
            connectTimestamp = System.currentTimeMillis();
            managerExecutor.execute(connectRunnable());
        }
    }

    @Override
    public void addConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.add(listener);
    }

    @Override
    public void removeConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.remove(listener);
    }


    @Override
    public void shutdown() {
        int currentState = state.getAndSet(4);
        if (currentState != 4) {
            reConnector.stopDisconnect();
            reConnector.stopReconnect();
            ConnectionManager.getInstance().removeConnection(this);
        }
        if (currentState == 2) {
            managerExecutor.execute(disconnectRunnable(Event.SHUT_DOWN));
        }
    }

    void disconnect(Event event) {
        if (state.compareAndSet(2, 3)) {
            managerExecutor.execute(disconnectRunnable(event));
        }
    }

    public void onReady() {
        if (state.get() == 2) {
            Logger.i("Connection is ready," + connectionInfo);
            reConnector.onReady();
            if (connectEventListeners.size() > 0) {
                callbackExecutor.execute(() -> {
                    for (ConnectEventListener listener : connectEventListeners) {
                        listener.onReady();
                    }
                });
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return !reConnector.isConnectFailed();
    }

    public void onConnect() {
        Logger.i("Connection is established, " + connectionInfo);
        reConnector.onConnect();
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnect();
                }
            });
        }
    }

    public void onDisconnect(Event event) {
        Logger.i("Connection is disconnected, " + connectionInfo);
        boolean isNetworkAvailable = ConnectionManager.getInstance().isNetworkAvailable();
        if (isNetworkAvailable) {
            reConnector.onDisconnect(event);
        }

        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onDisconnect(event);
                }
            });
        }
    }

    public void onConnectFailed() {
        Logger.i("Fail to establish connection, " + connectionInfo);
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            reConnector.onConnectFailed();
        }
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnectFailed();
                }
            });
        }
    }

    @Override
    public void onNetworkStateChanged(boolean available) {
        if (available) {
            reConnector.reset();
            reConnector.reconnectDelay();
        } else {
            disconnect(Event.NETWORK_NOT_AVAILABLE);
        }
    }

    public void setBackground() {
        timestamp = System.currentTimeMillis();
        reConnector.disconnectDelay();
    }


    public void setForeground() {
        timestamp = 0;
        pulse.pulseOnce();
        reConnector.stopDisconnect();
    }

    boolean isSleep() {
        return timestamp != 0
                && System.currentTimeMillis() - timestamp > options.getBackgroundLiveTime() * 1000;
    }

    public abstract TaskManager<T, ? extends Task<?>> taskManager();

    abstract Runnable connectRunnable();

    protected abstract Runnable disconnectRunnable(Event event);
}
