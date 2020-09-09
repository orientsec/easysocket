package com.orientsec.easysocket.client;

import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.utils.Logger;
import com.orientsec.easysocket.utils.NetUtils;

import static com.orientsec.easysocket.client.EasySocketClient.RESTART;

class Connector {

    private final Options options;

    private final EventManager eventManager;

    private final Logger logger;

    private EasySocketClient socketClient;

    Connector(EasySocketClient socketClient) {
        this.socketClient = socketClient;
        options = socketClient.getOptions();
        logger = options.getLogger();
        eventManager = socketClient.eventManager;
    }


    /**
     * 停止重连任务
     */
    void stopRestart() {
        eventManager.remove(RESTART);
    }

    void prepareRestart() {
        if (needConnect() &&
                NetUtils.isNetworkAvailable(EasySocket.getInstance().getContext())) {
            stopRestart();

            long delay = options.getConnectInterval();
            eventManager.publish(RESTART, delay);
            logger.i("Restart after " + delay + " mill seconds...");
        }
    }


    void restart() {
        if (needConnect()) {
            socketClient.onStart(false);
        } else {
            logger.i("Restart abandon.");
        }
    }

    private boolean needConnect() {
        return socketClient.session == null
                && options.getLivePolicy().autoConnect(socketClient.isActive());
    }

}
