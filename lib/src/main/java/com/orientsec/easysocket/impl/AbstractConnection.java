package com.orientsec.easysocket.impl;


import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.Event;
import com.orientsec.easysocket.utils.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

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
    final byte[] lock = new byte[0];

    enum State {
        IDLE, STARTING, CONNECT, AVAILABLE, STOPPING, SHUTDOWN
    }

    /**
     * 0 空闲状态 0 -> 1
     * 1 连接中 1 -> 0; 1 -> 2
     * 2 连接成功 2 -> 3
     * 3
     * 3 连接断开中 3 -> 0
     * 4 关闭 0 -> 4; 1 -> 4; 2 -> 4; 3 -> 4
     * <p>
     * 连接状态
     */
    State state = State.IDLE;

    private volatile long timestamp;

    volatile long connectTimestamp;

    private Set<ConnectEventListener> connectEventListeners = new CopyOnWriteArraySet<>();

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
        return state == State.SHUTDOWN;
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (state == State.IDLE
                    && ConnectionManager.getInstance().isNetworkAvailable()) {
                state = State.STARTING;
                connectTimestamp = System.currentTimeMillis();
                managerExecutor.execute(connectRunnable());
            }
        }
    }

    @Override
    public void shutdown() {
        if (state == State.SHUTDOWN) return;
        synchronized (lock) {
            if (state != State.SHUTDOWN) {
                reConnector.stopDisconnect();
                reConnector.stopReconnect();
                ConnectionManager.getInstance().removeConnection(this);
            }
            if (state == State.CONNECT || state == State.AVAILABLE) {
                managerExecutor.execute(disconnectRunnable(Event.SHUT_DOWN));
            }
            state = State.SHUTDOWN;
        }
    }

    void disconnect(Event event) {
        synchronized (lock) {
            if (state == State.CONNECT || state == State.AVAILABLE) {
                managerExecutor.execute(disconnectRunnable(event));
                state = State.STOPPING;
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    public void onAvailable() {
        Logger.i("Connection is available," + connectionInfo);
        reConnector.onAvailable();
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onAvailable();
                }
            });
        }
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
        reConnector.onDisconnect(event);

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
        reConnector.onConnectFailed();
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
        return System.currentTimeMillis() - timestamp > options.getBackgroundLiveTime() * 1000
                && timestamp != 0;
    }

    @Override
    public void addConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.add(listener);
    }

    @Override
    public void removeConnectEventListener(@NonNull ConnectEventListener listener) {
        connectEventListeners.remove(listener);
    }


    public abstract TaskManager<T, ? extends Task<?>> taskManager();

    abstract Runnable connectRunnable();

    protected abstract Runnable disconnectRunnable(Event event);
}
