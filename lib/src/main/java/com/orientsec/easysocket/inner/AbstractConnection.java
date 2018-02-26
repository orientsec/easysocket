package com.orientsec.easysocket.inner;

import android.support.annotation.NonNull;

import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.LivePolicy;
import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.utils.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public abstract class AbstractConnection implements Connection, ConnectionManager.OnNetworkStateChangedListener {
    /**
     * 0 空闲状态 0 -> 1; 0 -> 4
     * 1 连接中 1 -> 0; 1 -> 2; 1 -> 4
     * 2 连接成功 2 -> 3; 2 -> 4
     * 3 连接断开中 3 -> 0; 3 -> 4
     * 4 关闭
     * <p>
     * 可能的状态：
     */
    protected AtomicInteger state = new AtomicInteger();

    private volatile long timestamp;

    private Set<ConnectEventListener> connectEventListeners = Collections.synchronizedSet(new HashSet<>());

    private Connector connector;

    protected ConnectionInfo connectionInfo;

    protected Options options;

    protected Pulse pulse;

    private ScheduledExecutorService executorService;


    public Options options() {
        return options;
    }

    protected AbstractConnection(Options options) {
        this.options = options;
        executorService = options.getExecutorService();
        pulse = new Pulse(this);
        connector = new Connector();
        connectionInfo = options.getConnectionInfo();
    }

    public Pulse pulse() {
        return pulse;
    }

    public abstract void onPulse(Message message);

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
            connector.stopDisconnect();
            connector.stopReconnect();
            ConnectionManager.getInstance().removeConnection(this);
        }
        if (currentState == 2) {
            doOnDisconnect();
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
    }

    protected void sendConnectEvent() {
        Logger.i("connection is established!");
        connector.onConnect();
        if (connectEventListeners.size() > 0) {
            options.getDispatchExecutor().execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnect();
                }
            });
        }
    }

    protected void sendDisconnectEvent() {
        Logger.i("connection is disconnected!");
        boolean isNetworkAvailable = ConnectionManager.getInstance().isNetworkAvailable();
        if (isNetworkAvailable) {
            connector.onDisconnect();
        }

        if (connectEventListeners.size() > 0) {
            options.getDispatchExecutor().execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onDisconnect();
                    if (!isNetworkAvailable) {
                        listener.onConnectFailed();
                    }
                }
            });
        }
    }

    protected void sendConnectFailedEvent() {
        Logger.i("connection fail to establish!");
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            connector.onConnectFailed();
        }
        if (connectEventListeners.size() > 0) {
            options.getDispatchExecutor().execute(() -> {
                for (ConnectEventListener listener : connectEventListeners) {
                    listener.onConnectFailed();
                }
            });
        }
    }

    @Override
    public void onNetworkStateChanged(boolean available) {
        if (available) {
            connector.reset();
            connector.reconnectDelay(1);
        } else {
            disconnect();
        }
    }

    public void setBackground() {
        timestamp = System.currentTimeMillis();
        connector.disconnectDelay();
    }


    public void setForeground() {
        timestamp = 0;
        pulse.pulseOnce();
        connector.stopDisconnect();
    }

    private boolean isSleep() {
        return timestamp != 0 && System.currentTimeMillis() - timestamp > options.getBackgroundLiveTime() * 1000;
    }


    private class Connector implements ConnectEventListener {
        private final Object lock = new byte[0];
        /**
         * 默认重连时间(后面会以指数次增加)
         */
        private static final int DEFAULT = 3;
        /**
         * 最大连接失败次数,不包括断开异常
         */
        private static final int MAX_CONNECTION_FAILED_TIMES = 5;
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

        private Future<?> disconnectTask;

        @Override
        public void onConnect() {
            reset();
            if (isSleep()) {
                disconnectDelay();
            }
        }

        @Override
        public void onDisconnect() {
            reconnectDelay(DEFAULT);
        }

        @Override
        public void onConnectFailed() {
            //连接失败达到阈值,需要切换备用线路
            if (++connectionFailedTimes >= MAX_CONNECTION_FAILED_TIMES) {
                reset();
                List<ConnectionInfo> connectionInfoList = options.getBackupConnectionInfoList();
                if (connectionInfoList != null && connectionInfoList.size() > 0) {
                    if (++backUpIndex >= connectionInfoList.size()) {
                        Logger.i("switch to main server");
                        backUpIndex = -1;
                        connectionInfo = options.getConnectionInfo();
                    } else {
                        Logger.i("switch to backup server");
                        connectionInfo = connectionInfoList.get(backUpIndex);
                    }
                }
            }
            reconnectDelay(reconnectTimeDelay);
            reconnectTimeDelay = reconnectTimeDelay * 2;//x+2x+4x

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
            if (state.get() != 0 || isSleep()) {
                return;
            }
            synchronized (lock) {
                stopReconnect();
                Logger.i(" reconnect after " + second + " seconds...");
                reconnectTask = executorService.schedule(() -> {
                    if (!isSleep()) {
                        connect();
                    }
                    reconnectTask = null;
                }, second, TimeUnit.SECONDS);
            }
        }

        private void stopDisconnect() {
            synchronized (lock) {
                if (disconnectTask != null) {
                    disconnectTask.cancel(true);
                    disconnectTask = null;
                }
            }
        }

        private void disconnectDelay() {
            if (isShutdown() || options.getLivePolicy() != LivePolicy.STRONG) {
                return;
            }
            synchronized (lock) {
                stopDisconnect();
                disconnectTask = executorService.schedule(() -> {
                    if (isSleep()) {
                        Logger.i("will disconnect, state: sleep");
                        stopReconnect();
                        disconnect();
                    }
                    disconnectTask = null;
                }, options.getBackgroundLiveTime(), TimeUnit.SECONDS);
            }
        }
    }

    protected abstract Message buildMessage(MessageType messageType);
}
