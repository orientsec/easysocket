package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.session.Session;
import com.orientsec.easysocket.error.EasyException;

/**
 * Interface `ConnectionListener` defines callbacks for monitoring connection events.
 * It provides methods to handle various connection states such as starting, success, failure,
 * availability, and disconnection.
 */
public interface ConnectionListener {

    /**
     * Called when a connection attempt starts.
     *
     * @param session The session associated with the connection.
     */
    void onConnecting(@NonNull final Session session);

    /**
     * Called when a connection is successfully established.
     *
     * @param session The session associated with the successful connection.
     */
    void onConnected(@NonNull final Session session);

    /**
     * Called when a connection attempt fails.
     * This can occur due to server issues or network problems.
     *
     * @param session The session associated with the failed connection.
     * @param e       The exception describing the failure.
     */
    void onConnectFailed(@NonNull final Session session, @NonNull EasyException e);

    /**
     * Called when the client successfully logs into the server.
     * All requests can only be initiated after a successful login.
     * This event confirms that the client has successfully connected to the server,
     * even if the connection might be interrupted later.
     *
     * @param session The session associated with the connection.
     */
    void onAvailable(@NonNull final Session session);

    /**
     * Called when the connection is terminated.
     *
     * @param session The session associated with the terminated connection.
     * @param e       The exception describing the reason for disconnection.
     */
    void onDisconnected(@NonNull final Session session, @NonNull EasyException e);
}