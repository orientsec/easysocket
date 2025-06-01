package com.orientsec.easysocket.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.Period;
import com.orientsec.easysocket.task.TaskBuilder;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Represents a session interface that extends the `PacketHandler` interface.
 * Provides methods to check connection status, retrieve connection details,
 * and measure connection times.
 */
public interface Session extends PacketHandler {

    /**
     * Checks if the session is connected.
     *
     * @return `true` if the session is connected, otherwise `false`.
     */
    boolean isConnect();

    /**
     * Checks if the connection is reachable.
     *
     * @return `true` if the connection is reachable, otherwise `false`.
     */
    boolean isAvailable();

    /**
     * Checks if the server is available.
     * The server is considered available if the session successfully enters
     * the {@link State#AVAILABLE} state while the network is accessible.
     *
     * @return `true` if the server is available, otherwise `false`.
     */
    boolean isServerAvailable();

    /**
     * Retrieves the current connection site information.
     *
     * @return An `Address` object representing the current connection site information.
     */
    @NonNull
    Address getAddress();

    /**
     * Retrieves the IP address of the site.
     *
     * @return An `InetAddress` object representing the site IP, or `null` if unavailable.
     */
    @Nullable
    InetAddress getInetAddress();

    /**
     * Retrieves the index of the site address.
     *
     * @return An integer representing the index of the site address.
     */
    int getAddressIndex();

    /**
     * Retrieves the connection time in milliseconds.
     *
     * @return A long value representing the time taken to establish the socket connection.
     */
    long connectTime();

    /**
     * Retrieves the connection time for specific phases in milliseconds.
     *
     * @param period A `Period` object representing the specific connection phase.
     * @return A long value representing the time taken for the specified connection phase.
     */
    long connectTime(Period period);
}
