package com.orientsec.easysocket.inner;

import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.Errors;
import com.orientsec.easysocket.utils.Logger;
import com.orientsec.easysocket.utils.NetUtils;

import java.util.List;

public class Connector implements EventListener {

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

    Connector(EasySocket easySocket) {
        this.easySocket = easySocket;
        logger = easySocket.getLogger();
        eventManager = easySocket.getEventManager();
    }

    void attach(RealConnection connection) {
        this.connection = connection;
    }

    void reset() {
        failedTimes = 0;
        prepareAutoStop();
    }

    /**
     * 停止重连任务
     */
    void stopRestart() {
        eventManager.remove(Events.RESTART);
    }

    /**
     * 停止断开连接任务
     */
    void stopAutoStop() {
        eventManager.remove(Events.AUTO_STOP);
    }

    void prepareRestart() {
        List<Address> addressList = connection.addressList;
        if (addressList == null) return;
        switchServer(addressList);

        if (needConnect() && NetUtils.isNetworkAvailable(easySocket.getContext())) {
            stopRestart();

            long delay = easySocket.getConnectInterval();
            eventManager.publish(Events.RESTART, delay);
            logger.i("Reconnect after " + delay + " mill seconds...");
        }
    }

    /**
     * 切换服务器
     */
    private void switchServer(List<Address> addressList) {
        //连接失败达到阈值,需要切换备用线路
        if (++failedTimes >= easySocket.getRetryTimes()) {
            failedTimes = 0;

            if (++backUpIndex >= addressList.size()) {
                backUpIndex = 0;
                Address address = addressList.get(backUpIndex);
                connection.address = address;
                logger.i("Switch to server, " + address);
            }
        }
    }

    private void restart() {
        if (needConnect()) {
            connection.onStart();
        }
    }

    private boolean needConnect() {
        return connection.state == State.IDLE
                && easySocket.getLivePolicy().autoConnect(connection.isSleep());
    }

    void prepareAutoStop() {
        if (connection.isShutdown() || !easySocket.getLivePolicy().autoDisconnect()) {
            return;
        }
        stopAutoStop();
        long delay = easySocket.getBackgroundLiveTime();
        eventManager.publish(Events.AUTO_STOP, delay);
        logger.i("Disconnect after " + delay + " mill seconds...");
    }

    private void autoStop() {
        if (easySocket.getLivePolicy().autoDisconnect() && connection.isSleep()) {
            logger.i("App is sleeping, stop connection.");
            stopRestart();
            connection.onStop(Errors.systemError(ErrorCode.SLEEP, "App is sleeping."));
        }
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        switch (eventId) {
            case Events.RESTART:
                restart();
                break;
            case Events.AUTO_STOP:
                autoStop();
                break;
        }
    }
}
