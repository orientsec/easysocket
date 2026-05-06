package com.orientsec.easysocket.session

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.PacketHandler
import com.orientsec.easysocket.Period
import java.net.InetAddress

/**
 * Represents a session interface that extends the `PacketHandler` interface.
 * Provides methods to check connection status, retrieve connection details,
 * and measure connection times.
 */
interface Session : PacketHandler {

    /**
     * Checks if the session is connected.
     *
     * @return `true` if the session is connected, otherwise `false`.
     */
    val isConnect: Boolean

    /**
     * Checks if the connection is reachable.
     *
     * @return `true` if the connection is reachable, otherwise `false`.
     */
    val isAvailable: Boolean

    /**
     * Checks if the server is available.
     * The server is considered available if the session successfully enters
     * the [State.AVAILABLE] state while the network is accessible.
     *
     * @return `true` if the server is available, otherwise `false`.
     */
    val isServerAvailable: Boolean

    /**
     * Retrieves the current connection site information.
     *
     * @return An `Address` object representing the current connection site information.
     */
    val address: Address

    /**
     * Retrieves the IP address of the site.
     *
     * @return An `InetAddress` object representing the site IP, or `null` if unavailable.
     */
    val inetAddress: InetAddress?

    /**
     * Retrieves the index of the site address.
     *
     * @return An integer representing the index of the site address.
     */
    val addressIndex: Int

    /**
     * Retrieves the connection time in milliseconds.
     *
     * @return A long value representing the time taken to establish the socket connection.
     */
    fun connectTime(): Long

    /**
     * Retrieves the connection time for specific phases in milliseconds.
     *
     * @param period A `Period` object representing the specific connection phase.
     * @return A long value representing the time taken for the specified connection phase.
     */
    fun connectTime(period: Period): Long
}
