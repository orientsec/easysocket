package com.orientsec.easysocket.inner;

import android.support.annotation.NonNull;

import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.LivePolicy;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.utils.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 15:13
 * Author: Fredric
 * coding is art not science
 */
public abstract class AbstractConnection implements Connection {
    protected final Object lock = new byte[0];
    /**
     * 0 空闲状态 0 -> 1; 0 -> 4
     * 1 连接中 1-> 2; 1-> 4
     * 2 连接成功 2 -> 3; 2 -> 4
     * 3 连接断开中 3 -> 1; 3 -> 4
     * 4 关闭
     * <p>
     * 可能的状态：
     */
    protected AtomicInteger state = new AtomicInteger();

    protected volatile boolean sleep;

    private Set<ConnectEventListener> connectEventListeners = Collections.synchronizedSet(new HashSet<>());

    private Reconnection reconnection;

    protected ConnectionInfo connectionInfo;

    protected Options options;

    protected Pulse pulse;

    private ScheduledExecutorService executorService;

    private Future<?> disconnectTask;

    public Options options() {
        return options;
    }

    protected AbstractConnection(Options options) {
        this.options = options;
        executorService = Executors.newSingleThreadScheduledExecutor();
        pulse = new Pulse(this);
        reconnection = new Reconnection();
        connectionInfo = options.getConnectionInfo();
    }

    public ScheduledExecutorService executorService() {
        return executorService;
    }

    public Pulse pulse() {
        return pulse;
    }

    public abstract void onPulse(SendMessage message);

    public abstract TaskExecutor<? extends Task> taskExecutor();

    @Override
    public boolean isShutdown() {
        return state.get() == 4;
    }

    @Override
    public void start() {
        connect();
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
            ConnectionManager.getInstance().removeConnection(this);
        }
        if (currentState == 2) {
            doOnDisconnect();
        }
        if (currentState == 1) {
            reconnection.stopReconnect();
        }
    }

    public void connect() {
        if (state.compareAndSet(0, 1)) {
            doOnConnect();
        }
    }

    public void disconnect() {
        if (state.compareAndSet(2, 3)) {
            doOnDisconnect();
        }
    }

    /**
     * 启动建立连接任务
     */
    protected abstract void doOnConnect();

    /**
     * 启动断开连接任务,停止心跳、清理连接资源
     */
    protected void doOnDisconnect() {
        //停止心跳
        pulse.stop();
        //取消断开连接的任务
        cancelDisconnectTask();
    }


    protected void sendDisconnectEvent() {
        reconnection.onDisconnect();
        if (connectEventListeners.size() > 0) {
            options.getDispatchExecutor().execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onDisconnect();
                }
            });
        }
    }

    protected void sendConnectEvent() {
        reconnection.onConnect();
        if (connectEventListeners.size() > 0) {
            options.getDispatchExecutor().execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnect();
                }
            });
        }
    }

    protected void sendConnectFailedEvent() {
        reconnection.onConnectFailed();
        if (connectEventListeners.size() > 0) {
            options.getDispatchExecutor().execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnectFailed();
                }
            });
        }
    }

    public void setBackground() {
        synchronized (lock) {
            cancelDisconnectTask();
            if (isConnect() && options.getLivePolicy() != LivePolicy.STRONG) {
                disconnectTask = executorService.schedule(() -> {
                    if (ConnectionManager.getInstance().isBackground()) {
                        synchronized (lock) {
                            sleep = true;
                            reconnection.stopReconnect();
                        }
                        disconnect();
                    }
                    disconnectTask = null;
                }, options.getBackgroundLiveTime(), TimeUnit.SECONDS);
            }
        }
    }

    private void cancelDisconnectTask() {
        synchronized (lock) {
            if (disconnectTask != null) {
                disconnectTask.cancel(true);
                disconnectTask = null;
            }
        }
    }

    public void setForeground() {
        synchronized (lock) {
            sleep = false;
            cancelDisconnectTask();
        }
    }


    private class Reconnection implements ConnectEventListener {
        /**
         * 默认重连时间(后面会以指数次增加)
         */
        private static final int DEFAULT = 5;
        /**
         * 最大连接失败次数,不包括断开异常
         */
        private static final int MAX_CONNECTION_FAILED_TIMES = 12;
        /**
         * 延时连接时间
         */
        private int reconnectTimeDelay = DEFAULT;
        /**
         * 连接失败次数,不包括断开异常
         */
        private int connectionFailedTimes = 0;
        /**
         * 备用站点下标
         */
        private int backUpIndex = -1;

        private Future<?> reconnectTask;

        @Override
        public void onDisconnect() {
            reconnectDelay(DEFAULT);
        }

        @Override
        public void onConnect() {
            reset();
        }

        @Override
        public void onConnectFailed() {
            if (++connectionFailedTimes > MAX_CONNECTION_FAILED_TIMES) {
                reset();
                //连接失败达到阈值,需要切换备用线路
                List<ConnectionInfo> connectionInfoList = options.getBackupConnectionInfoList();
                if (connectionInfoList != null && connectionInfoList.size() > 0) {
                    if (++backUpIndex >= connectionInfoList.size()) {
                        backUpIndex = -1;
                        connectionInfo = options.getConnectionInfo();
                    } else {
                        connectionInfo = connectionInfoList.get(backUpIndex);
                    }
                    //站点切换，2s后开始连接
                    reconnectDelay(2);
                    return;
                }
            }
            reconnectDelay(reconnectTimeDelay);
            reconnectTimeDelay = reconnectTimeDelay * 2;//5+10+20+40 = 75 4次
            if (reconnectTimeDelay >= 60) {
                reconnectTimeDelay = DEFAULT;
            }
        }

        private void stopReconnect() {
            synchronized (lock) {
                if (reconnectTask != null) {
                    reconnectTask.cancel(true);
                    reconnectTask = null;
                }
            }
        }


        private void reset() {
            reconnectTimeDelay = DEFAULT;
            connectionFailedTimes = 0;
        }

        private void reconnectDelay(int second) {
            synchronized (lock) {
                if (sleep && (state.compareAndSet(3, 0) || state.compareAndSet(1, 0))) {
                    Logger.i("connection is sleeping!");
                } else if (state.compareAndSet(3, 1) || state.get() == 1) {
                    Logger.i(" reconnect after " + second + " s...");
                    reconnectTask = executorService.schedule(() -> {
                        if (sleep && state.compareAndSet(1, 0)) {
                            Logger.i("connection is sleeping!");
                        } else if (state.get() == 1) {
                            doOnConnect();
                        } else {
                            Logger.i("connection is already shutdown!");
                        }
                        reconnectTask = null;
                    }, second, TimeUnit.SECONDS);
                } else if (state.get() == 4) {
                    Logger.i("connection is already shutdown!");
                } else {
                    //this should never happen
                    throw new IllegalStateException();
                }
            }
        }
    }
}
