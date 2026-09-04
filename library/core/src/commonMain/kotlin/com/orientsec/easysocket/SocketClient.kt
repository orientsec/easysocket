package com.orientsec.easysocket

import com.orientsec.easysocket.push.PushManager
import com.orientsec.easysocket.session.Session
import com.orientsec.easysocket.task.TaskBuilder
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.CoroutineScope

/**
 * This interface defines the contract for a socket client.
 * It extends the [TaskBuilder] interface, allowing for the creation and execution of tasks.
 * The SocketClient provides methods for managing the connection lifecycle (start, stop, shutdown),
 * checking connection status (isShutdown, isConnected, isAvailable),
 * managing connection listeners, accessing the push manager, options, logger, session, and address
 * list.
 */
interface SocketClient : TaskBuilder {
    /**
     * Starts and active the connection. If the connection is already started, this has no effect.
     */
    fun start()

    /**
     * Stops the current connection.
     */
    fun stop()

    /**
     * Shuts down the connection. After shutdown, the connection is no longer available.
     */
    fun shutdown()

    /**
     * Checks if the connection is shut down.
     *
     * @return True if the connection is shut down, false otherwise.
     */
    fun isShutdown(): Boolean

    /**
     * Checks if the client is connected.
     *
     * @return True if the client is connected, false otherwise.
     */
    fun isConnected(): Boolean

    /**
     * Checks if the connection is available.
     *
     * @return True if the connection is available, false otherwise.
     */
    fun isAvailable(): Boolean

    /**
     * Adds a connection event listener.
     *
     * @param listener The listener to be added.
     */
    fun addConnectionListener(listener: ConnectionListener)

    /**
     * Removes a connection event listener.
     *
     * @param listener The listener to be removed.
     */
    fun removeConnectionListener(listener: ConnectionListener)

    /**
     * Sets the list of server addresses.
     *
     * @param addressList The list of server addresses.
     */
    fun setAddressList(addressList: List<Address>)

    /**
     * The push manager associated with the client.
     */
    val pushManager: PushManager<*, *>?

    /**
     * The options associated with the current connection.
     */
    val options: Options

    /**
     * The logger for the client.
     */
    val logger: Logger

    /**
     * The coroutine scope for the client.
     */
    val scope: CoroutineScope

    /**
     * The current session of the connection.
     */
    val session: Session?
}
