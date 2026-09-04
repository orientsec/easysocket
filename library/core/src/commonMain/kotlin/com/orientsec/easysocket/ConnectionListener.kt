package com.orientsec.easysocket

import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.session.Session

/**
 * Interface `ConnectionListener` defines callbacks for monitoring connection events.
 * It provides methods to handle various connection states such as starting, success, failure,
 * availability, and disconnection.
 */
interface ConnectionListener {

    /**
     * Called when a connection attempt starts.
     *
     * @param session The session associated with the connection.
     */
    fun onConnecting(session: Session)

    /**
     * Called when a connection is successfully established.
     *
     * @param session The session associated with the successful connection.
     */
    fun onConnected(session: Session)

    /**
     * Called when a connection attempt fails.
     * This can occur due to server issues or network problems.
     *
     * @param session The session associated with the failed connection.
     * @param e       The exception describing the failure.
     */
    fun onConnectFailed(session: Session, e: EasyException)

    /**
     * Called when the client successfully logs into the server.
     * All requests can only be initiated after a successful login.
     * This event confirms that the client has successfully connected to the server,
     * even if the connection might be interrupted later.
     *
     * @param session The session associated with the connection.
     */
    fun onAvailable(session: Session)

    /**
     * Called when the connection is terminated.
     *
     * @param session The session associated with the terminated connection.
     * @param e       The exception describing the reason for disconnection.
     */
    fun onDisconnected(session: Session, e: EasyException)
    /**
     * Called when the network becomes available.
     */
    fun onNetworkAvailable() {}
}
