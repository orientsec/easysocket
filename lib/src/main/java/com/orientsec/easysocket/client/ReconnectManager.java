package com.orientsec.easysocket.client;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.session.Session;
import com.orientsec.easysocket.utils.Logger;

/**
 * The `Reconnect` class handles the reconnection logic for the Socket client.
 * It implements the `Runnable` interface to execute reconnection tasks under specific conditions.
 */
class ReconnectManager implements Runnable {
    // Configuration options for the Socket client
    private final Options options;
    // Logger for logging messages
    private final Logger logger;
    // Associated Socket client instance
    private final EasySocketClient socketClient;

    /**
     * Constructor to initialize the `Reconnect` instance.
     *
     * @param socketClient The associated Socket client instance
     */
    ReconnectManager(EasySocketClient socketClient) {
        this.socketClient = socketClient;
        options = socketClient.getOptions();
        logger = socketClient.logger;
    }

    /**
     * Executes a delayed reconnection after a session connection fails or disconnects.
     *
     * @param session The current failed session instance
     */
    void delayedReconnect(@NonNull Session session) {
        // If the current session's server is unavailable, switch to the next server
        if (!session.isServerAvailable()) {
            socketClient.switchServer();
        }
        // Check the reconnection policy to determine if reconnection is needed
        if (options.getLivePolicy().shouldReconnect(socketClient.isActive())) {
            EasyExecutor mainExecutor = socketClient.getMainExecutor();
            mainExecutor.remove(this); // Remove the current task
            long delay = options.getConnectIntervalInMills(); // Get the reconnection interval
            mainExecutor.schedule(this, delay); // Schedule the reconnection task
            logger.i("restart after " + delay + " mill seconds...");
        }
    }

    /**
     * Performs an immediate reconnection.
     */
    void reconnect() {
        // Check the reconnection policy to determine if reconnection is needed
        if (options.getLivePolicy().shouldReconnect(socketClient.isActive())) {
            socketClient.onStart(false); // Start the Socket client
        } else {
            logger.i("restart canceled...");
        }
    }

    /**
     * The `run` method of the `Runnable` interface, which executes the reconnection logic.
     */
    @Override
    public void run() {
        reconnect();
    }
}
