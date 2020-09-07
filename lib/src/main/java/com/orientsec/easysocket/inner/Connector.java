package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.BuildConfig;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.utils.Logger;
import com.orientsec.easysocket.utils.NetUtils;

import java.util.List;

class Connector {

    private final EasySocket easySocket;

    private final EventManager eventManager;

    private final Logger logger;

    private RealConnection connection;

    /**
     * 连接失败次数,不包括断开异常
     */
    private int failedTimes = 0;
    /**
     * 备用站点下标
     */
    private int backUpIndex = -1;

    Connector(RealConnection connection) {
        this.connection = connection;
        easySocket = connection.getEasySocket();
        logger = easySocket.getLogger();
        eventManager = easySocket.getEventManager();
    }

    void ready() {
        failedTimes = 0;
    }

    /**
     * 停止重连任务
     */
    void stopRestart() {
        eventManager.remove(Events.RESTART);
    }

    void prepareRestart() {
        if (needConnect() && NetUtils.isNetworkAvailable(easySocket.getContext())) {
            stopRestart();

            long delay = easySocket.getConnectInterval();
            eventManager.publish(Events.RESTART, delay);
            logger.i("Restart after " + delay + " mill seconds...");
        }
    }

    /**
     * 切换服务器
     */
    void switchServer() {
        List<Address> addressList = connection.addressList;
        if (BuildConfig.DEBUG && (addressList == null || addressList.isEmpty())) {
            throw new AssertionError("Assertion failed");
        }

        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= easySocket.getRetryTimes()) {
            failedTimes = 0;

            if (++backUpIndex >= addressList.size()) {
                backUpIndex = 0;
                Address address = addressList.get(backUpIndex);
                connection.address = address;
                logger.i("Switch to server: " + address);
            }
        }
    }

    void restart() {
        if (needConnect()) {
            connection.onStart();
        } else {
            logger.i("Restart abandon.");
        }
    }

    private boolean needConnect() {
        return connection.state == State.IDLE
                && easySocket.getLivePolicy().autoConnect(connection.isActive());
    }

}
