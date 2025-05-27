package com.orientsec.easysocket.client;


import com.orientsec.easysocket.EasyRunner;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.utils.Logger;

class Connector implements Runnable {

    private final Options options;

    private final Logger logger;

    private final EasySocketClient socketClient;

    Connector(EasySocketClient socketClient) {
        this.socketClient = socketClient;
        options = socketClient.getOptions();
        logger = socketClient.logger;
    }


    /**
     * Session连接失败或者断开后执行延时的重连。
     */
    void restart(Session session) {
        //if (EasySocket.getInstance().isNetworkAvailable()) {
        //当前Session连接的服务器不可用的情况下，需要切换到下一个站点。
        if (!session.isServerAvailable()) {
            socketClient.switchServer();
        }
        if (options.getLivePolicy().autoConnect(socketClient.isActive())) {
            EasyRunner runner = socketClient.getEasyRunner();
            runner.remove(this, this);
            long delay = options.getConnectInterval();
            runner.postDelayed(this, this, delay);
            logger.i("restart after " + delay + " mill seconds...");
        }
        //}
    }


    /**
     * 重连。
     */
    void restart() {
        if (options.getLivePolicy().autoConnect(socketClient.isActive())) {
            socketClient.onStart(false);
        } else {
            logger.i("restart abandoned");
        }
    }

    @Override
    public void run() {
        restart();
    }
}
