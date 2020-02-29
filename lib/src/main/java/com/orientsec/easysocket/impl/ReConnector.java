package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.LivePolicy;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.Error;
import com.orientsec.easysocket.utils.Logger;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReConnector<T> implements ConnectEventListener {

    private Options<T> options;

    private final AbstractConnection connection;

    private ScheduledExecutorService scheduler;

    /**
     * 连接失败次数,不包括断开异常
     */
    private int failedTimes = 0;
    /**
     * 备用站点下标
     */
    private int backUpIndex = -1;

    private Future<?> reconnectTask;

    private Future<?> disconnectTask;

    ReConnector(AbstractConnection<T> connection) {
        this.connection = connection;
        options = connection.options;
        scheduler = connection.options.getScheduledExecutor();
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect(EasyException e) {
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            switchServer();
            reconnectDelay();
        }
    }

    @Override
    public void onConnectFailed() {
        if (ConnectionManager.getInstance().isNetworkAvailable()) {
            switchServer();
            reconnectDelay();
        }
    }

    @Override
    public void onAvailable() {
        failedTimes = 0;
        if (connection.isSleep()) {
            disconnectDelay();
        }
    }

    /**
     * 切换服务器
     */
    private void switchServer() {
        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= options.getRetryTimes()) {
            failedTimes = 0;

            List<Address> addressList = options.getBackupAddressList();
            if (addressList != null && addressList.size() > 0) {
                if (++backUpIndex >= addressList.size()) {
                    Logger.i("switch to main server");
                    backUpIndex = -1;
                    connection.address = options.getAddress();
                } else {
                    Logger.i("switch to backup server");
                    connection.address = addressList.get(backUpIndex);
                }
            }
        }
    }

    /**
     * 停止重连任务
     */
    void stopReconnect() {
        if (reconnectTask != null) {
            reconnectTask.cancel(true);
            reconnectTask = null;
        }
    }

    /**
     * 停止断开连接任务
     */
    void stopDisconnect() {
        if (disconnectTask != null) {
            disconnectTask.cancel(true);
            disconnectTask = null;
        }
    }

    void reconnectDelay() {
        if (connection.state != AbstractConnection.State.IDLE || connection.isSleep()) {
            return;
        }
        stopReconnect();
        long delay = options.getConnectInterval();
        Logger.i("Reconnect after " + delay + " mill seconds...");
        Runnable reconnect = () -> {
            synchronized (connection) {
                if (!connection.isSleep()) {
                    connection.start();
                }
                reconnectTask = null;
            }
        };
        reconnectTask = scheduler.schedule(reconnect, delay, TimeUnit.MILLISECONDS);
    }

    void disconnectDelay() {
        if (connection.isShutdown() || options.getLivePolicy() != LivePolicy.STRONG) {
            return;
        }
        if (disconnectTask != null) return;
        long delay = options.getBackgroundLiveTime();
        Logger.i("Reconnect after " + delay + " seconds...");
        Runnable disconnect = () -> {
            synchronized (connection) {
                if (connection.isSleep()) {
                    Logger.i("App is sleeping, stop connection.");
                    stopReconnect();
                    connection.disconnect(Error.create(Error.Code.SLEEP));
                }
                disconnectTask = null;
            }
        };
        disconnectTask = scheduler.schedule(disconnect, delay, TimeUnit.SECONDS);
    }
}
