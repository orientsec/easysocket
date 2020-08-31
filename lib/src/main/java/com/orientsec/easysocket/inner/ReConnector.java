package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectionManager;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.ErrorCode;
import com.orientsec.easysocket.exception.ErrorType;
import com.orientsec.easysocket.utils.Logger;

import java.util.List;

public class ReConnector<T> {

    private final Options<T> options;

    private final EasyConnection<T> connection;

    private final EventManager eventManager;

    /**
     * 连接失败次数,不包括断开异常
     */
    private int failedTimes = 0;
    /**
     * 备用站点下标
     */
    private int backUpIndex = -1;

    ReConnector(EasyConnection<T> connection) {
        this.connection = connection;
        eventManager = connection.eventManager;
        options = connection.options;
    }

    void reset() {
        failedTimes = 0;
        disconnect();
    }

    /**
     * 切换服务器
     */
    private void switchServer(List<Address> addressList) {
        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= options.getRetryTimes()) {
            failedTimes = 0;

            if (++backUpIndex >= addressList.size()) {
                backUpIndex = 0;
                Address address = addressList.get(backUpIndex);
                connection.address = address;
                Logger.i("Switch to server, " + address);
            }
        }
    }

    /**
     * 停止重连任务
     */
    void stopReconnect() {
        eventManager.remove(Events.START_DELAY);
    }

    /**
     * 停止断开连接任务
     */
    void stopDisconnect() {
        eventManager.remove(Events.STOP_DELAY);
    }

    void reconnectDelay() {
        List<Address> addressList = connection.addressList;
        if (addressList == null) return;
        switchServer(addressList);

        if (needConnect()
                && ConnectionManager.getInstance().isNetworkAvailable()) {
            stopReconnect();

            long delay = options.getConnectInterval();
            eventManager.publish(Events.START_DELAY, delay);
            Logger.i("Reconnect after " + delay + " mill seconds...");
        }
    }

    void reconnect() {
        if (needConnect()) {
            connection.onStart();
        }
    }

    private boolean needConnect() {
        return connection.state == State.IDLE
                && options.getLivePolicy().autoConnect(connection.isForeground());
    }

    void disconnectDelay() {
        if (connection.isShutdown() || !options.getLivePolicy().autoDisconnect()) {
            return;
        }
        stopDisconnect();
        long delay = options.getBackgroundLiveTime();
        eventManager.publish(Events.STOP_DELAY, delay);
        Logger.i("Disconnect after " + delay + " mill seconds...");
    }

    void disconnect() {
        if (options.getLivePolicy().autoDisconnect() && connection.isSleep()) {
            Logger.i("App is sleeping, stop connection.");
            stopReconnect();
            EasyException e = new EasyException(ErrorCode.SLEEP,
                    ErrorType.SYSTEM, "App is sleeping.");
            connection.onStop(e);
        }
    }
}
