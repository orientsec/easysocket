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
    /**
     * 连接状态
     * <p>
     * IDLE
     * 空闲状态 IDLE -> STARTING
     * <p>
     * STARTING 启动中
     * 1.STARTING -> IDLE (建连失败)
     * 2.STARTING -> CONNECT (建连成功)
     * <p>
     * CONNECT 连接成功
     * 1.CONNECT -> AVAILABLE (初始化成功)
     * 2.CONNECT -> IDLE (初始化失败)
     * <p>
     * AVAILABLE 连接可用
     * AVAILABLE -> IDLE (连接断开)
     * <p>
     * STOPPING 连接断开中
     * STOPPING -> IDLE  (连接断开)
     * <p>
     * SHUTDOWN 关闭，关闭之后连接不再可用。
     */
    enum State {
        IDLE, STARTING, CONNECT, AVAILABLE, STOPPING, SHUTDOWN
    }

    /**
     * 连接状态
     */
    volatile State state = State.IDLE;

    private volatile long timestamp;

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
    public synchronized void start() {
        if (state == State.IDLE
                && ConnectionManager.getInstance().isNetworkAvailable()) {
            state = State.STARTING;
            managerExecutor.execute(this::connectRunnable);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (state == State.SHUTDOWN) return;
        if (state != State.SHUTDOWN) {
            reConnector.stopDisconnect();
            reConnector.stopReconnect();
            ConnectionManager.getInstance().removeConnection(this);
        }
        if (state == State.CONNECT || state == State.AVAILABLE) {
            managerExecutor.execute(() -> disconnectRunnable(Event.SHUT_DOWN));
        }
        state = State.SHUTDOWN;
    }

    synchronized void disconnect(Event event) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            managerExecutor.execute(() -> disconnectRunnable(event));
            state = State.STOPPING;
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
    public synchronized void onNetworkStateChanged(boolean available) {
        if (available) {
            reConnector.reconnectDelay();
        } else {
            disconnect(Event.NETWORK_NOT_AVAILABLE);
        }
    }

    public synchronized void setBackground() {
        timestamp = System.currentTimeMillis();
        reConnector.disconnectDelay();
    }


    public synchronized void setForeground() {
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

    abstract void connectRunnable();

    abstract void disconnectRunnable(Event event);
}
