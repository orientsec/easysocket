package com.orientsec.easysocket.impl;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.utils.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;


/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 15:13
 * Author: Fredric
 * coding is art not science
 */
public abstract class AbstractConnection<T> implements Connection<T>, ConnectEventListener {
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

    private ReConnector<T> reConnector;

    Address address;

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
        address = options.getAddressList().get(0);
        callbackExecutor = options.getCallbackExecutor();
        managerExecutor = options.getManagerExecutor();
    }

    @Override
    public boolean isShutdown() {
        return state == State.SHUTDOWN;
    }

    @Override
    public synchronized void start() {
        if (state == State.IDLE) {
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
        EasyException e = new EasyException(ErrorCode.SHUT_DOWN,
                ErrorType.SYSTEM, "Connection shut down.");
        disconnect(e);
        state = State.SHUTDOWN;
    }

    synchronized void disconnect(EasyException e) {
        if (state == State.CONNECT || state == State.AVAILABLE) {
            managerExecutor.execute(() -> disconnectRunnable(e));
            state = State.STOPPING;
        }
    }

    @Override
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    public void onAvailable() {
        Logger.i("Connection is available," + address);
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
        Logger.i("Connection is established, " + address);
        reConnector.onConnect();
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnect();
                }
            });
        }
    }

    public void onDisconnect(@NonNull EasyException e) {
        Logger.i("Connection is disconnected, " + address);
        reConnector.onDisconnect(e);

        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onDisconnect(e);
                }
            });
        }
    }

    public void onConnectFailed() {
        Logger.i("Fail to establish connection, " + address);
        reConnector.onConnectFailed();
        if (connectEventListeners.size() > 0) {
            callbackExecutor.execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnectFailed();
                }
            });
        }
    }

    public synchronized void onNetworkAvailable() {
        reConnector.reconnectDelay();
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
        return System.currentTimeMillis() - timestamp > options.getBackgroundLiveTime()
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


    public abstract TaskManager<T, ? extends Task<T, ?>> taskManager();

    abstract void connectRunnable();

    abstract void disconnectRunnable(EasyException e);
}
