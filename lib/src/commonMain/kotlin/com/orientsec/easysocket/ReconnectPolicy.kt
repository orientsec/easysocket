package com.orientsec.easysocket

/**
 * Enum `ReconnectPolicy` defines the reconnection strategies for controlling
 * whether to automatically reconnect under different application states.
 *
 * Reconnection strategies include:
 * - [NONE]: No reconnection strategy.
 * - [ACTIVE]: Automatically reconnect when the application is active,
 *   but not when it is in a dormant state.
 * - [ALWAYS]: Always automatically reconnect, regardless of the
 *   application's state.
 */
enum class ReconnectPolicy {
    /**
     * No reconnection strategy.
     *
     * In this strategy, the application will not attempt to reconnect automatically.
     */
    NONE,

    /**
     * Reconnect only when the application is active.
     *
     * In this strategy:
     * - Automatically reconnect when the application is active.
     * - No automatic reconnection when the application is dormant.
     */
    ACTIVE,

    /**
     * Always reconnect automatically.
     *
     * In this strategy, the application will attempt to reconnect automatically
     * regardless of whether it is active or dormant.
     */
    ALWAYS;

    /**
     * Determines whether to reconnect automatically based on the current policy and
     * application state.
     *
     * @param active Indicates whether the application is active.
     * @return `true` if the application should reconnect automatically, otherwise `false`.
     */
    fun shouldReconnect(active: Boolean): Boolean {
        return when (this) {
            NONE -> false
            ACTIVE -> active
            ALWAYS -> true
        }
    }
}
