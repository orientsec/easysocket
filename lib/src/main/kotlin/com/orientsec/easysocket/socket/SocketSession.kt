package com.orientsec.easysocket.socket

import android.net.TrafficStats
import com.orientsec.easysocket.Address
import com.orientsec.easysocket.Options
import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.PacketHandler
import com.orientsec.easysocket.PacketType
import com.orientsec.easysocket.Period
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.BlockingReader
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.Pulse
import com.orientsec.easysocket.socket.QueuedWriter
import com.orientsec.easysocket.session.State
import com.orientsec.easysocket.task.Callback
import com.orientsec.easysocket.task.Task
import com.orientsec.easysocket.task.TaskImpl
import com.orientsec.easysocket.task.TaskManager
import com.orientsec.easysocket.task.TaskType
import com.orientsec.easysocket.utils.LogFactory
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.EnumMap
import javax.net.ssl.SSLSocket

/**
 * Represents a socket session that manages the lifecycle, connection state,
 * and data read/write operations of a socket. Implements the `OperableSession`
 * and `Runnable` interfaces.
 */
class SocketSession(
    private val socketClient: BaseSocketClient,
    override val address: Address,
    override val addressIndex: Int,
    private val id: Long
) : OperableSession {

    // Suffix information for identifying the session
    override val suffix: String = ("  session(" + id + ")[" + address.host + ":"
            + address.port + "]  client[" + socketClient.options.name + "]")

    // The socket used for the session
    private var mSocket: Socket? = null

    // Reader for blocking data reads
    private var reader: BlockingReader? = null

    // Writer for queued data writes
    override var writer: QueuedWriter? = null
        private set

    // Configuration options for the session
    private val options: Options = socketClient.options

    // Logger for logging session-related messages
    override val logger: Logger = LogFactory.getLogger(options, suffix)

    // Task manager for managing session tasks
    private val taskManager: TaskManager = socketClient.getTaskManager()

    // Current state of the session
    private var state = State.IDLE

    override val isConnect: Boolean
        get() = state == State.CONNECTED || state == State.AVAILABLE

    override val isAvailable: Boolean
        get() = state == State.AVAILABLE

    override val isServerAvailable: Boolean
        get() = serverAvailable

    // Flag indicating whether the server is available
    private var serverAvailable = false

    // Heartbeat manager for the session
    private var pulse: Pulse? = null

    // Map of message handlers for processing packets
    private val messageHandlerMap: MutableMap<PacketType, PacketHandler> =
        EnumMap(PacketType::class.java)

    /**
     * Stores the connection time for various phases.
     */
    private val connectTimeMap: MutableMap<Period, Long> = EnumMap(Period::class.java)

    /**
     * Builds a task for the session.
     *
     * @param request  The request object.
     * @param callback The callback object.
     * @param <T>      The type of the task result.
     * @return A new task instance.
     */
    override fun <T> buildTask(
        request: Request<T>,
        callback: Callback<T>
    ): Task<T> {
        return TaskImpl(
            TaskType.INITIALIZE, taskManager.generateTaskId(),
            request, callback, socketClient, this
        )
    }

    /**
     * Retrieves the writer for the session.
     *
     * @return The writer instance, or `null` if not initialized.
     */

    /**
     * Retrieves the logger for the session.
     *
     * @return The logger instance.
     */

    /**
     * Retrieves the suffix information for the session.
     *
     * @return The suffix string.
     */

    /**
     * Handles an incoming packet.
     *
     * @param packet The received packet.
     */
    override fun handlePacket(packet: Packet) {
        socketClient.scope.launch { onPacket(packet) }
    }

    /**
     * Internal method for processing a received packet.
     *
     * @param packet The received packet.
     */
    private fun onPacket(packet: Packet) {
        if (state == State.DETACHED) return
        val packetHandler = messageHandlerMap[packet.packetType]
        if (packetHandler == null) {
            logger.w("no packet handler for " + packet.packetType)
        } else {
            logger.d("receive a packet: $packet")
            packetHandler.handlePacket(packet)
        }
    }

    /**
     * Opens the session and starts the connection process.
     * Changes the session state to `STARTING` and executes the connection task.
     */
    override fun open() {
        if (state == State.IDLE) {
            state = State.STARTING
            logger.i("session is opening")
            socketClient.onConnecting(this)

            socketClient.scope.launch {
                performConnect()
            }
        }
    }

    /**
     * Closes the session with the specified exception.
     * If the session is in the `IDLE` or `STARTING` state, it transitions to `DETACHED`.
     * Otherwise, it handles the error.
     *
     * @param e The exception that caused the session to close.
     */
    override fun close(e: EasyException) {
        if (state == State.IDLE || state == State.STARTING) {
            state = State.DETACHED
            logger.i("session is closed, error: " + e.message)
            socketClient.onConnectFailed(this, e)
        } else {
            onError(e)
        }
    }

    /**
     * Prepares the session after a successful connection.
     * Initializes resources, starts the reader and writer, and updates the session state.
     *
     * @param socket The connected socket.
     */
    private suspend fun onReady(socket: Socket) {
        if (state == State.STARTING) {
            this.mSocket = socket
            val sessionScope = socketClient.scope

            // Start the reader and writer
            reader = BlockingReader(this, socket, socketClient)
            reader?.start(sessionScope)

            val queuedWriter = QueuedWriter(this, socket, sessionScope)
            writer = queuedWriter

            messageHandlerMap[PacketType.RESPONSE] = taskManager

            state = State.CONNECTED
            logger.i("session start success")

            socketClient.onConnected(this)
            // Perform pre-connection operations, such as resource initialization
            val initializer = socketClient.sessionInitializer
            if (initializer == null) {
                onAvailable()
            } else {
                initializer.start(this)
                    .onSuccess { onAvailable() }
                    .onFailure {
                        logger.e("fail to initialize session, error: " + it.message)
                        val e = EasyException.Companion.invoke(
                            ErrorCode.SESSION_INIT_FAILED, ErrorType.CONNECT,
                            "session initializing failed", suffix, it
                        )
                        onError(e)
                    }
            }
        } else {
            // Session is already closed
            withContext(Dispatchers.IO) {
                try {
                    socket.close()
                } catch (ioe: IOException) {
                    logger.d("socket is closed ", ioe)
                }
            }
        }
    }

    /**
     * Handles a failed connection attempt by transitioning the session state to `DETACHED`
     * and notifying the socket client of the failure.
     *
     * @param e The exception that caused the failure.
     */
    private fun onFailed(e: EasyException) {
        if (state == State.STARTING) {
            state = State.DETACHED
            logger.i("session start failed, error: " + e.message)

            socketClient.onConnectFailed(this, e)
        }
    }

    /**
     * Marks the session as available, starts the heartbeat, and registers message handlers.
     * Updates the session state to `AVAILABLE`.
     */
    private fun onAvailable() {
        if (state == State.CONNECTED) {
            // Start the heartbeat
            pulse = Pulse(socketClient, this)
            pulse?.start()
            // Register handlers for heartbeat and push messages
            messageHandlerMap[PacketType.PULSE] = pulse!!
            val pushManager = socketClient.pushManager
            if (pushManager != null) {
                messageHandlerMap[PacketType.PUSH] = pushManager
            }
            state = State.AVAILABLE
            serverAvailable = true
            logger.i("session is available")

            socketClient.onAvailable(this)
        }
    }

    /**
     * Handles an error by stopping the session, shutting down resources, and notifying the socket client.
     *
     * @param e The exception that caused the error.
     */
    private fun onError(e: EasyException) {
        if (state == State.CONNECTED || state == State.AVAILABLE) {
            pulse?.stop()
            reader?.shutdown()
            writer?.shutdown()
            socketClient.scope.launch(Dispatchers.IO) {
                try {
                    mSocket?.close()
                } catch (ioe: IOException) {
                    logger.d("socket is closed ", ioe)
                }
            }

            state = State.DETACHED
            logger.i("session is closed, error: " + e.message)

            socketClient.onDisconnected(this, e)
        }
    }

    /**
     * Starts the socket connection process.
     * Measures connection times for DNS resolution, connection establishment, and SSL handshake.
     */
    private suspend fun performConnect() {
        logger.d("socket connection is starting")
        TrafficStats.setThreadStatsTag(options.connectStatsTag)
        try {
            val socket = withContext(Dispatchers.IO) {
                val s = socketClient.socketFactory.createSocket()
                s.tcpNoDelay = true
                s.keepAlive = true
                s.setPerformancePreferences(1, 2, 0)

                val startTimeMill = System.currentTimeMillis()
                var timestamp = startTimeMill
                // STEP 1: DNS resolution
                val socketAddress: SocketAddress = InetSocketAddress(address.host, address.port)
                var currentTimeMillis = System.currentTimeMillis()
                connectTimeMap[Period.DNS] = currentTimeMillis - timestamp
                timestamp = currentTimeMillis

                // STEP 2: Connection establishment
                s.connect(socketAddress, options.connectTimeoutMillis)
                currentTimeMillis = System.currentTimeMillis()
                connectTimeMap[Period.CONNECT] = currentTimeMillis - timestamp
                timestamp = currentTimeMillis

                // STEP 3: SSL handshake
                if (s is SSLSocket) {
                    s.startHandshake()
                    currentTimeMillis = System.currentTimeMillis()
                    connectTimeMap[Period.SSL] = currentTimeMillis - timestamp
                    timestamp = currentTimeMillis
                }

                // Total connection time
                val connectTime = timestamp - startTimeMill
                connectTimeMap[Period.ALL] = connectTime
                logger.d("socket connected in " + connectTime + "ms")
                s
            }
            onReady(socket)
        } catch (e: Exception) {
            logger.w("socket connection start failed ", e)
            val error = EasyException.Companion.invoke(
                ErrorCode.SOCKET_CONNECT,
                ErrorType.CONNECT, "socket connection failed", suffix, e
            )
            onFailed(error)
        } finally {
            TrafficStats.clearThreadStatsTag()
        }
    }

    /**
     * Checks if the session is connected.
     *
     * @return `true` if the session is connected, otherwise `false`.
     */

    /**
     * Checks if the session is available.
     *
     * @return `true` if the session is available, otherwise `false`.
     */

    /**
     * Checks if the server is available.
     *
     * @return `true` if the server is available, otherwise `false`.
     */

    /**
     * Retrieves the address information for the session.
     *
     * @return The address object.
     */

    /**
     * Retrieves the IP address of the session.
     *
     * @return The IP address, or `null` if the socket is not initialized.
     */
    override val inetAddress: InetAddress?
        get() = mSocket?.inetAddress

    /**
     * Retrieves the index of the address.
     *
     * @return The address index.
     */

    /**
     * Retrieves the total connection time.
     *
     * @return The total connection time in milliseconds, or `-1` if not available.
     */
    override fun connectTime(): Long {
        val time = connectTimeMap[Period.ALL]
        return time ?: -1
    }

    /**
     * Retrieves the connection time for a specific phase.
     *
     * @param period The connection phase.
     * @return The connection time in milliseconds, or `-1` if not available.
     */
    override fun connectTime(period: Period): Long {
        val time = connectTimeMap[period]
        return time ?: -1
    }

    /**
     * Returns a string representation of the session.
     *
     * @return A string containing the session ID and address.
     */
    override fun toString(): String {
        return ("SocketSession[" +
                "id=" + id +
                "address=" + address +
                ']')
    }

}