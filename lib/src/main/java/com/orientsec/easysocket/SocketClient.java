package com.orientsec.easysocket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.session.Session;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.task.TaskBuilder;
import com.orientsec.easysocket.utils.Logger;

import java.util.List;

/**
 * This interface defines the contract for a socket client.
 * It extends the {@link TaskBuilder} interface, allowing for the creation and execution of tasks.
 * The SocketClient provides methods for managing the connection lifecycle (start, stop, shutdown),
 * checking connection status (isShutdown, isConnected, isAvailable),
 * managing connection listeners, accessing the push manager, options, logger, session, and address
 * list.
 */
public interface SocketClient extends TaskBuilder {
    /**
     * Starts and active the connection. If the connection is already started, this has no effect.
     */
    void start();

    /**
     * Stops the current connection.
     */
    void stop();

    /**
     * Shuts down the connection. After shutdown, the connection is no longer available.
     */
    void shutdown();

    /**
     * Checks if the connection is shut down.
     *
     * @return True if the connection is shut down, false otherwise.
     */
    boolean isShutdown();

    /**
     * Checks if the client is connected.
     *
     * @return True if the client is connected, false otherwise.
     */
    boolean isConnected();

    /**
     * Checks if the connection is available.
     *
     * @return True if the connection is available, false otherwise.
     */
    boolean isAvailable();

    /**
     * Adds a connection event listener.
     *
     * @param listener The listener to be added.
     */
    void addConnectionListener(@NonNull ConnectionListener listener);

    /**
     * Removes a connection event listener.
     *
     * @param listener The listener to be removed.
     */
    void removeConnectionListener(@NonNull ConnectionListener listener);

    /**
     * Retrieves the push manager associated with the client.
     *
     * @return The push manager, or null if not available.
     */
    @Nullable
    PushManager<?, ?> getPushManager();

    /**
     * Retrieves the options associated with the current connection.
     *
     * @return The options object.
     */
    @NonNull
    Options getOptions();

    /**
     * Retrieves the logger for the client.
     *
     * @return The logger instance.
     */
    @NonNull
    Logger getLogger();

    /**
     * Retrieves the current session of the connection.
     *
     * @return The current session, or null if no session exists.
     */
    @Nullable
    Session getSession();

    /**
     * Retrieves the list of server addresses.
     *
     * @return The list of addresses, or null if not available.
     */
    @Nullable
    List<Address> getAddressList();

    /**
     * Sets the list of server addresses.
     *
     * @param addressList The list of addresses to be set.
     */
    void setAddressList(@NonNull List<Address> addressList);
}