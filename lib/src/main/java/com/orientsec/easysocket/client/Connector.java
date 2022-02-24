package com.orientsec.easysocket.client;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.utils.Logger;

import static com.orientsec.easysocket.client.EasySocketClient.RESTART;

class Connector {

    private final Options options;

    private final EventManager eventManager;

    private final Logger logger;

    private final EasySocketClient socketClient;

    Connector(EasySocketClient socketClient) {
        this.socketClient = socketClient;
        options = socketClient.getOptions();
        logger = options.getLogger();
        eventManager = socketClient.eventManager;
    }


    /**
     * 重连
     */
    void prepareRestart() {
        if (options.getLivePolicy().autoConnect(socketClient.isActive())) {
            eventManager.remove(RESTART);

            long delay = options.getConnectInterval();
            eventManager.publish(RESTART, delay);
            logger.i("Restart after " + delay + " mill seconds...");
        }
    }


    void restart() {
        if (socketClient.session == null
                && options.getLivePolicy().autoConnect(socketClient.isActive())) {
            socketClient.onStart(false);
        } else {
            logger.i("Restart abandon.");
        }
    }

}
