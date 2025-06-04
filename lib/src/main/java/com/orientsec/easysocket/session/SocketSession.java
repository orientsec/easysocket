package com.orientsec.easysocket.session;

import android.net.TrafficStats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.PacketType;
import com.orientsec.easysocket.Period;
import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.task.Callback;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskImpl;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.task.TaskType;
import com.orientsec.easysocket.utils.LogFactory;
import com.orientsec.easysocket.utils.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSocket;

/**
 * Represents a socket session that manages the lifecycle, connection state,
 * and data read/write operations of a socket. Implements the `OperableSession`
 * and `Runnable` interfaces.
 */
public class SocketSession implements OperableSession, Runnable {
    // Suffix information for identifying the session
    private final String suffix;

    // The socket used for the session
    private Socket mSocket;

    // Reader for blocking data reads
    private BlockingReader reader;

    // Writer for queued data writes
    private QueuedWriter writer;

    // Executor for handling connection tasks
    private final Executor connectExecutor;

    // Configuration options for the session
    private final Options options;

    // Main thread task scheduler
    private final EasyExecutor mainExecutor;

    // Logger for logging session-related messages
    private final Logger logger;

    // The associated socket client
    private final BaseSocketClient socketClient;

    // Task manager for managing session tasks
    private final TaskManager taskManager;

    // Address information for the session
    private final Address address;

    // Index of the address
    private final int addressIndex;

    // Current state of the session
    private State state = State.IDLE;

    // Flag indicating whether the server is available
    private boolean serverAvailable = false;

    // Heartbeat manager for the session
    private Pulse pulse;

    // Map of message handlers for processing packets
    private final Map<PacketType, PacketHandler> messageHandlerMap = new HashMap<>();

    // Unique identifier for the session
    private final long id;

    /**
     * Stores the connection time for various phases.
     */
    private final Map<Period, Long> connectTimeMap = new HashMap<>();

    /**
     * Constructs a new `SocketSession` instance.
     *
     * @param socketClient The associated socket client.
     * @param address      The address information for the session.
     * @param addressIndex The index of the address.
     * @param id           The unique identifier for the session.
     */
    public SocketSession(BaseSocketClient socketClient, Address address, int addressIndex, long id) {
        this.socketClient = socketClient;
        this.address = address;
        this.addressIndex = addressIndex;
        this.options = socketClient.getOptions();
        this.id = id;
        this.connectExecutor = options.getConnectExecutor();
        this.mainExecutor = socketClient.getMainExecutor();
        // Initialize the task manager
        this.taskManager = socketClient.getTaskManager();

        this.suffix = "  session(" + id + ")[" + address.getHost() + ":"
                + address.getPort() + "]  client[" + options.getName() + "]";
        this.logger = LogFactory.getLogger(options, suffix);
    }

    /**
     * Builds a task for the session.
     *
     * @param request  The request object.
     * @param callback The callback object.
     * @param <T>      The type of the task result.
     * @return A new task instance.
     */
    @NonNull
    @Override
    public <T> Task<T> buildTask(@NonNull Request<T> request,
                                 @NonNull Callback<T> callback) {
        return new TaskImpl<>(TaskType.INITIALIZE, taskManager.generateTaskId(),
                request, callback, socketClient, this);
    }

    /**
     * Retrieves the writer for the session.
     *
     * @return The writer instance, or `null` if not initialized.
     */
    @Nullable
    @Override
    public Writer getWriter() {
        return writer;
    }

    /**
     * Retrieves the logger for the session.
     *
     * @return The logger instance.
     */
    @Override
    @NonNull
    public Logger getLogger() {
        return logger;
    }

    /**
     * Retrieves the suffix information for the session.
     *
     * @return The suffix string.
     */
    @NonNull
    @Override
    public String getSuffix() {
        return suffix;
    }

    /**
     * Handles an incoming packet.
     *
     * @param packet The received packet.
     */
    @Override
    public void handlePacket(@NonNull Packet packet) {
        mainExecutor.execute(() -> onPacket(packet));
    }

    /**
     * Internal method for processing a received packet.
     *
     * @param packet The received packet.
     */
    private void onPacket(@NonNull Packet packet) {
        if (state == State.DETACHED) return;
        PacketHandler packetHandler = messageHandlerMap.get(packet.getPacketType());
        if (packetHandler == null) {
            logger.w("no packet handler for " + packet.getPacketType());
        } else {
            logger.d("receive a packet: " + packet);
            packetHandler.handlePacket(packet);
        }
    }

    /**
     * Opens the session and starts the connection process.
     * Changes the session state to `STARTING` and executes the connection task.
     */
    @Override
    public void open() {
        if (state == State.IDLE) {
            connectExecutor.execute(this);
            state = State.STARTING;
            logger.i("session is opening");

            socketClient.onConnecting(this);
        }
    }

    /**
     * Closes the session with the specified exception.
     * If the session is in the `IDLE` or `STARTING` state, it transitions to `DETACHED`.
     * Otherwise, it handles the error.
     *
     * @param e The exception that caused the session to close.
     */
    @Override
    public void close(EasyException e) {
        if (state == State.IDLE || state == State.STARTING) {
            state = State.DETACHED;
            logger.i("session is closed, error: " + e.getMessage());
            socketClient.onConnectFailed(this, e);
        } else {
            onError(e);
        }
    }

    /**
     * Prepares the session after a successful connection.
     * Initializes resources, starts the reader and writer, and updates the session state.
     *
     * @param socket The connected socket.
     */
    private void onReady(Socket socket) {
        if (state == State.STARTING) {
            this.mSocket = socket;
            // Start the reader and writer threads
            reader = new BlockingReader(this, socket, socketClient);
            reader.start();
            writer = new QueuedWriter(this, socket, socketClient);

            messageHandlerMap.put(PacketType.RESPONSE, taskManager);

            state = State.CONNECTED;
            logger.i("session start success");

            socketClient.onConnected(this);
            // Perform pre-connection operations, such as resource initialization
            SessionInitializer initializer = socketClient.getSessionInitializer();
            if (initializer == null) {
                onAvailable();
            } else {
                initializer.start(new InitializeEmitter(), this);
            }
        } else {
            // Session is already closed
            connectExecutor.execute(() -> {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    logger.d("socket is closed ", ioe);
                }
            });
        }
    }

    /**
     * Handles a failed connection attempt by transitioning the session state to `DETACHED`
     * and notifying the socket client of the failure.
     *
     * @param e The exception that caused the failure.
     */
    private void onFailed(EasyException e) {
        if (state == State.STARTING) {
            state = State.DETACHED;
            logger.i("session start failed, error: " + e.getMessage());

            socketClient.onConnectFailed(this, e);
        }
    }

    /**
     * Marks the session as available, starts the heartbeat, and registers message handlers.
     * Updates the session state to `AVAILABLE`.
     */
    private void onAvailable() {
        if (state == State.CONNECTED) {
            // Start the heartbeat
            pulse = new Pulse(socketClient, this, mainExecutor);
            pulse.start();
            // Register handlers for heartbeat and push messages
            messageHandlerMap.put(PacketType.PULSE, pulse);
            PushManager<?, ?> pushManager = socketClient.getPushManager();
            if (pushManager != null) {
                messageHandlerMap.put(PacketType.PUSH, pushManager);
            }
            state = State.AVAILABLE;
            serverAvailable = true;
            logger.i("session is available");

            socketClient.onAvailable(this);
        }
    }

    /**
     * Handles an error by stopping the session, shutting down resources, and notifying the socket client.
     *
     * @param e The exception that caused the error.
     */
    private void onError(EasyException e) {
        if (state == State.CONNECTED || state == State.AVAILABLE) {
            if (pulse != null) {
                pulse.stop();
            }
            reader.shutdown();
            writer.cancelAll();
            connectExecutor.execute(() -> {
                try {
                    mSocket.close();
                } catch (IOException ioe) {
                    logger.d("socket is closed ", ioe);
                }
            });

            state = State.DETACHED;
            logger.i("session is closed, error: " + e.getMessage());

            socketClient.onDisconnected(this, e);
        }
    }

    /**
     * Starts the socket connection process.
     * Measures connection times for DNS resolution, connection establishment, and SSL handshake.
     */
    @Override
    public void run() {
        logger.d("socket connection is starting");
        TrafficStats.setThreadStatsTag(options.getConnectStatsTag());
        try {
            Socket socket = socketClient.getSocketFactory().createSocket();
            // Disable Nagle's algorithm to send TCP packets immediately
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setPerformancePreferences(1, 2, 0);

            long startTimeMill = System.currentTimeMillis();
            long timestamp = startTimeMill;
            // STEP 1: DNS resolution
            SocketAddress socketAddress
                    = new InetSocketAddress(address.getHost(), address.getPort());
            long currentTimeMillis = System.currentTimeMillis();
            connectTimeMap.put(Period.DNS, currentTimeMillis - timestamp);
            timestamp = currentTimeMillis;

            // STEP 2: Connection establishment
            socket.connect(socketAddress, options.getConnectTimeOutInMills());
            currentTimeMillis = System.currentTimeMillis();
            connectTimeMap.put(Period.CONNECT, currentTimeMillis - timestamp);
            timestamp = currentTimeMillis;

            // STEP 3: SSL handshake
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).startHandshake();
                currentTimeMillis = System.currentTimeMillis();
                connectTimeMap.put(Period.SSL, currentTimeMillis - timestamp);
                timestamp = currentTimeMillis;
            }

            // Total connection time
            long connectTime = timestamp - startTimeMill;
            connectTimeMap.put(Period.ALL, connectTime);

            logger.d("socket connected in " + connectTime + "ms");
            mainExecutor.execute(() -> onReady(socket));
        } catch (Exception e) {
            logger.w("socket connection start failed ", e);
            EasyException error = EasyException.create(ErrorCode.SOCKET_CONNECT,
                    ErrorType.CONNECT, "socket connection failed", suffix, e);
            mainExecutor.execute(() -> onFailed(error));
        }
        TrafficStats.clearThreadStatsTag();
    }

    /**
     * Checks if the session is connected.
     *
     * @return `true` if the session is connected, otherwise `false`.
     */
    @Override
    public boolean isConnect() {
        return state == State.CONNECTED || state == State.AVAILABLE;
    }

    /**
     * Checks if the session is available.
     *
     * @return `true` if the session is available, otherwise `false`.
     */
    @Override
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    /**
     * Checks if the server is available.
     *
     * @return `true` if the server is available, otherwise `false`.
     */
    @Override
    public boolean isServerAvailable() {
        return serverAvailable;
    }

    /**
     * Retrieves the address information for the session.
     *
     * @return The address object.
     */
    @NonNull
    @Override
    public Address getAddress() {
        return address;
    }

    /**
     * Retrieves the IP address of the session.
     *
     * @return The IP address, or `null` if the socket is not initialized.
     */
    @Nullable
    @Override
    public InetAddress getInetAddress() {
        if (mSocket != null) {
            return mSocket.getInetAddress();
        }
        return null;
    }

    /**
     * Retrieves the index of the address.
     *
     * @return The address index.
     */
    @Override
    public int getAddressIndex() {
        return addressIndex;
    }

    /**
     * Retrieves the total connection time.
     *
     * @return The total connection time in milliseconds, or `-1` if not available.
     */
    @Override
    public long connectTime() {
        Long time = connectTimeMap.get(Period.ALL);
        if (time == null) {
            return -1;
        } else {
            return time;
        }
    }

    /**
     * Retrieves the connection time for a specific phase.
     *
     * @param period The connection phase.
     * @return The connection time in milliseconds, or `-1` if not available.
     */
    @Override
    public long connectTime(Period period) {
        Long time = connectTimeMap.get(period);
        if (time == null) {
            return -1;
        } else {
            return time;
        }
    }

    /**
     * Returns a string representation of the session.
     *
     * @return A string containing the session ID and address.
     */
    @NonNull
    @Override
    public String toString() {
        return "SocketSession[" +
                "id=" + id +
                "address=" + address +
                ']';
    }

    /**
     * The `InitializeEmitter` class is an implementation of the `SessionInitializer.Emitter` interface.
     * <p>
     * It is responsible for handling the results of the session initialization process.
     * This includes marking the session as available upon successful initialization
     * or handling errors when the initialization fails.
     */
    private class InitializeEmitter implements SessionInitializer.Emitter {

        /**
         * Marks the session as available by posting the availability task to the main thread.
         * <p>
         * This method is called when the session initialization process completes successfully.
         */
        @Override
        public void postSuccess() {
            mainExecutor.execute(SocketSession.this::onAvailable);
        }

        /**
         * Marks the session initialization as failed and handles the error.
         * <p>
         * This method is called when the session initialization process fails. It logs the error
         * and transitions the session to an error state.
         *
         * @param cause The cause of the failure, represented as a `Throwable` object.
         */
        @Override
        public void postFailure(@NonNull Throwable cause) {
            logger.e("fail to initialize session, error: " + cause.getMessage());
            EasyException e = EasyException.create(ErrorCode.SESSION_INIT_FAILED, ErrorType.CONNECT,
                    "session initializing failed", suffix, cause);
            mainExecutor.execute(() -> onError(e));
        }
    }
}