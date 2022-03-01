package com.orientsec.easysocket.client;

import static com.orientsec.easysocket.client.EasySocketClient.RESTART;

import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.utils.Logger;

class Connector {

    private final Options options;

    private final EventManager eventManager;

    private final Logger logger;

    private final EasySocketClient socketClient;

    Connector(EasySocketClient socketClient) {
        this.socketClient = socketClient;
        options = socketClient.getOptions();
        logger = socketClient.logger;
        eventManager = socketClient.eventManager;
    }


    /**
     * Session连接失败或者断开后执行延时的重连。
     */
    void restart(Session session) {
        if (EasySocket.getInstance().isNetworkAvailable()) {
            //当前Session连接的服务器不可用的情况下，需要切换到下一个站点。
            if (!session.isServerAvailable()) {
                socketClient.switchServer();
            }
            if (options.getLivePolicy().autoConnect(socketClient.isActive())) {
                eventManager.remove(RESTART);

                long delay = options.getConnectInterval();
                eventManager.publish(RESTART, delay);
                logger.i("Restart after " + delay + " mill seconds...");
            }
        }
    }


    /**
     * 重连。
     */
    void restart() {
        if (options.getLivePolicy().autoConnect(socketClient.isActive())) {
            socketClient.onStart(false);
        } else {
            logger.i("Restart abandon.");
        }
    }

}
