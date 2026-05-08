package com.orientsec.easysocket.utils

/**
 * Expected class for observing network state changes across platforms.
 */
expect class NetworkObserver {
    /**
     * Starts listening for network changes.
     * @param onAvailable Callback when network becomes available.
     */
    fun start(onAvailable: () -> Unit)

    /**
     * Stops listening for network changes.
     */
    fun stop()

    /**
     * Checks if the network is currently available.
     */
    fun isNetworkAvailable(): Boolean
}
