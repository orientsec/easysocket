package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.ConnectionInfo;
import com.orientsec.easysocket.LivePolicy;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.exception.Event;
import com.orientsec.easysocket.utils.Logger;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReConnector<T> implements ConnectEventListener {

    private Options<T> options;

    private AbstractConnection connection;

    private ScheduledExecutorService executorService;

    private final Object lock = new byte[0];
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

    private boolean connectFailed;

    ReConnector(AbstractConnection<T> connection) {
        this.connection = connection;
        options = connection.options;
        executorService = connection.options.getScheduledExecutor();
    }

    boolean isConnectFailed() {
        return connectFailed;
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect(Event event) {
        connectFailed = true;
        if (event.getCode() < 0 || ++connectionFailedTimes >= options.getRetryTimes()) {
            switchServer();
        }
        reconnectDelay();
    }

    @Override
    public void onConnectFailed() {
        connectFailed = true;
        //连接失败达到阈值,需要切换备用线路
        if (++connectionFailedTimes >= options.getRetryTimes()) {
            switchServer();
        }
        reconnectDelay();
        //reconnectTimeDelay = reconnectTimeDelay * 2;//x+2x+4x
    }

    @Override
    public void onReady() {
        connectFailed = false;
        reset();
        if (connection.isSleep()) {
            disconnectDelay();
        }
    }

    /**
     * 切换服务器
     */
    private void switchServer() {
        reset();
        List<ConnectionInfo> connectionInfoList = options.getBackupConnectionInfoList();
        if (connectionInfoList != null && connectionInfoList.size() > 0) {
            if (++backUpIndex >= connectionInfoList.size()) {
                Logger.i("switch to main server");
                backUpIndex = -1;
                connection.connectionInfo = options.getConnectionInfo();
            } else {
                Logger.i("switch to backup server");
                connection.connectionInfo = connectionInfoList.get(backUpIndex);
            }
        }
    }

    /**
     * 停止重连任务
     */
    void stopReconnect() {
        synchronized (lock) {
            if (reconnectTask != null) {
                reconnectTask.cancel(true);
                reconnectTask = null;
            }
        }
    }

    /**
     * 停止断开连接任务
     */
    void stopDisconnect() {
        synchronized (lock) {
            if (disconnectTask != null) {
                disconnectTask.cancel(true);
                disconnectTask = null;
            }
        }
    }

    void reset() {
        connectionFailedTimes = 0;
    }

    void reconnectDelay() {
        synchronized (lock) {
            if (connection.state.get() != 0 || connection.isSleep()) {
                return;
            }
            stopReconnect();
            long delay = options.getConnectInterval()
                    - (System.currentTimeMillis() - connection.connectTimestamp);
            Runnable reconnect = () -> {
                synchronized (lock) {
                    stopReconnect();
                    if (!connection.isSleep()) {
                        connection.start();
                    }
                }
            };
            if (delay > 0) {
                Logger.i("Reconnect after " + delay + " mill seconds...");
                reconnectTask = executorService.schedule(reconnect, delay, TimeUnit.MILLISECONDS);
            } else {
                Logger.i("Reconnect immediately.");
                reconnectTask = executorService.submit(reconnect);
            }
        }
    }

    void disconnectDelay() {
        if (connection.isShutdown() || options.getLivePolicy() != LivePolicy.STRONG) {
            return;
        }
        synchronized (lock) {
            stopDisconnect();
            disconnectTask = executorService.schedule(() -> {
                synchronized (lock) {
                    stopDisconnect();
                    if (connection.isSleep()) {
                        Logger.i("will disconnect, state: sleep");
                        stopReconnect();
                        connection.disconnect(Event.SLEEP);
                    }
                }
            }, options.getBackgroundLiveTime(), TimeUnit.SECONDS);
        }
    }
}
