package com.orientsec.easysocket;

/**
 * Enum `ReconnectPolicy` defines the reconnection strategies for controlling
 * whether to automatically reconnect under different application states.
 *
 * <p>Reconnection strategies include:
 * <ul>
 *     <li>{@link #NONE}: No reconnection strategy.</li>
 *     <li>{@link #ACTIVE}: Automatically reconnect when the application is active,
 *     but not when it is in a dormant state.</li>
 *     <li>{@link #ALWAYS}: Always automatically reconnect, regardless of the
 *     application's state.</li>
 * </ul>
 */
public enum ReconnectPolicy {

    /**
     * No reconnection strategy.
     * <p>In this strategy, the application will not attempt to reconnect automatically.</p>
     */
    NONE,

    /**
     * Reconnect only when the application is active.
     * <p>In this strategy:
     * <ul>
     *     <li>Automatically reconnect when the application is active.</li>
     *     <li>No automatic reconnection when the application is dormant.</li>
     * </ul>
     * </p>
     */
    ACTIVE,

    /**
     * Always reconnect automatically.
     * <p>In this strategy, the application will attempt to reconnect automatically
     * regardless of whether it is active or dormant.</p>
     */
    ALWAYS;

    /**
     * Determines whether to reconnect automatically based on the current policy and
     * application state.
     *
     * @param active Indicates whether the application is active.
     * @return `true` if the application should reconnect automatically, otherwise `false`.
     */
    public boolean shouldReconnect(boolean active) {
        if (this == NONE) return false;
        else if (this == ACTIVE) return active;
        else return true;
    }

}
