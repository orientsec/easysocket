package com.orientsec.easysocket.client

import com.orientsec.easysocket.session.Session
import kotlinx.coroutines.*

/**
 * The `ReconnectManager` class handles the reconnection logic for the Socket client.
 * It uses Kotlin Coroutines to manage delayed reconnection tasks.
 */
internal class ReconnectManager(private val socketClient: EasySocketClient) {
    // The policy that determines whether reconnection should occur.
    private val reconnectPolicy = socketClient.options.reconnectPolicy

    // The interval in milliseconds between connection attempts.
    private val connectIntervalMillis = socketClient.options.connectIntervalMillis.toLong()

    // Logger for logging messages
    private val logger = socketClient.logger

    // Job for managing the delayed reconnection coroutine
    private var reconnectJob: Job? = null

    /**
     * Executes a delayed reconnection after a session connection fails or disconnects.
     *
     * @param session The current failed session instance
     */
    fun delayedReconnect(session: Session) {
        // If the current session's server is unavailable, switch to the next server
        if (!session.isServerAvailable) {
            socketClient.switchServer()
        }
        // Check the reconnection policy to determine if reconnection is needed
        if (reconnectPolicy.shouldReconnect(socketClient.isActive())) {
            stop() // Cancel any pending reconnection
            reconnectJob = socketClient.scope.launch {
                logger.i("restart after $connectIntervalMillis mill seconds...")
                delay(connectIntervalMillis)
                reconnect()
            }
        } else {
            logger.i(
                "restart not needed, policy is $reconnectPolicy, " +
                        "active is ${socketClient.isActive()}"
            )
        }
    }

    /**
     * Performs an immediate reconnection.
     */
    fun reconnect() {
        // Check the reconnection policy to determine if reconnection is needed
        if (reconnectPolicy.shouldReconnect(socketClient.isActive())) {
            socketClient.onStart(false) // Start the Socket client
        } else {
            logger.i("restart canceled...")
        }
    }

    /**
     * Stops any pending reconnection task.
     */
    fun stop() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}
