package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.utils.Logger;
import com.orientsec.easysocket.utils.NetUtils;

class Connector {

    private final EasySocket easySocket;

    private final EventManager eventManager;

    private final Logger logger;

    private RealConnection connection;


    Connector(RealConnection connection) {
        this.connection = connection;
        easySocket = connection.getEasySocket();
        logger = easySocket.getLogger();
        eventManager = connection.eventManager;
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


    void restart() {
        if (needConnect()) {
            connection.onStart();
        } else {
            logger.i("Restart abandon.");
        }
    }

    private boolean needConnect() {
        return connection.session != null
                && easySocket.getLivePolicy().autoConnect(connection.isActive());
    }

}
