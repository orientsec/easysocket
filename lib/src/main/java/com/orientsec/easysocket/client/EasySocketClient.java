package com.orientsec.easysocket.client;


import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectionListener;
import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.EasySocket;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.task.Callback;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.session.OperableSession;
import com.orientsec.easysocket.session.Session;
import com.orientsec.easysocket.session.SocketSession;
import com.orientsec.easysocket.task.Task;
import com.orientsec.easysocket.task.TaskImpl;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.task.TaskManagerImpl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/**
 * EasySocketClient is a concrete implementation of {@link BaseSocketClient} that manages
 * socket connections, handles initialization, reconnection, and task execution.
 *
 * <p>This client maintains a connection to a server, selected from a list of addresses.
 * It supports features such as:
 * <ul>
 *     <li>Initialization of server addresses.</li>
 *     <li>Connection management including starting, stopping, and shutting down the connection.
 *     </li>
 *     <li>Automatic reconnection attempts upon connection failure or abortion.</li>
 *     <li>Task management for sending requests and receiving responses.</li>
 *     <li>Notifying connection listeners about connection state changes.</li>
 *     <li>Switching to backup servers if the primary server becomes unavailable.</li>
 *     <li>Managing client activity to optimize resource usage, especially when the application is
 *     in the background.</li>
 * </ul>
 *
 * <p>Key components:
 * <ul>
 *     <li>{@link ReconnectManager}: Handles the logic for reconnecting to the server.</li>
 *     <li>{@link TaskManager}: Manages pending and active tasks (requests).</li>
 *     <li>{@link SocketSession}: Represents the active connection to the server.</li>
 *     <li>{@link ConnectionListener}: Allows external components to listen for connection events.
 *     </li>
 *     <li>{@link Options}: Configuration for the client, including server addresses and retry
 *     policies.</li>
 *     <li>{@link EasyExecutor}: An executor for running client operations on a dedicated thread.</li>
 * </ul>
 *
 * <p>The client's lifecycle is managed through {@code start()}, {@code stop()}, and {@code
 * shutdown()} methods.
 * It uses an {@code activeTimestamp} to track client activity and determine if it should remain
 * active,
 * especially considering background application states.
 *
 * <p>Connection failures and disconnections trigger reconnection attempts, and if multiple
 * addresses are available,
 * the client can switch to an alternative server.
 */
public class EasySocketClient extends BaseSocketClient {
    // Represents the active state of the client.
    private static final int STATE_ACTIVE = 0;
    // Represents the sleep state of the client, typically when inactive.
    private static final int STATE_SLEEP = 1;
    // Represents the shutdown state of the client, indicating it is no longer operational.
    private static final int STATE_SHUTDOWN = 2;
    private final String name; // The name of the socket client.
    private final ReconnectManager reconnectManager; // Handles reconnection logic.
    private final TaskManager taskManager; // Manages tasks for requests and responses.
    private final Executor callbackExecutor; // Executor for running callbacks.
    // Listeners for connection events.
    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private int state = STATE_ACTIVE;
    private long activeTimestamp; // Timestamp of the last activation (e.g., a request was made).
    private SocketSession session; // Represents the current socket session.
    private Address currentAddress; // The currently active server address.
    private List<Address> addressList; // List of server addresses.
    private boolean isInitializing; // Indicates if initialization is in progress.
    private int failedTimes = 0; // Number of connection failures (excluding disconnections).
    private int addressIndex; // Index of the current server in the address list.
    private long sessionId; // Unique identifier for the session.

    /**
     * Constructs an EasySocketClient with the specified options and mainExecutor.
     *
     * @param options      Configuration options for the client.
     * @param mainExecutor Executor for running client operations.
     */
    public EasySocketClient(Options options, EasyExecutor mainExecutor) {
        super(options, mainExecutor);
        name = options.getName();
        callbackExecutor = options.getCallbackExecutor();
        taskManager = new TaskManagerImpl(logger);
        reconnectManager = new ReconnectManager(this);
    }

    /**
     * Builds a task for the given request and callback.
     *
     * @param request  The request to be executed.
     * @param callback The callback to handle the response.
     * @param <T>      The type of the response.
     * @return A new task instance.
     */
    @NonNull
    @Override
    public <T> Task<T> buildTask(@NonNull Request<T> request,
                                 @NonNull Callback<T> callback) {
        return new TaskImpl<>(taskManager.generateTaskId(), request, callback, this);
    }

    /**
     * Adds a connection listener to the client.
     *
     * @param listener The listener to be added.
     */
    @Override
    public void addConnectionListener(@NonNull ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Removes a connection listener from the client.
     *
     * @param listener The listener to be removed.
     */
    @Override
    public void removeConnectionListener(@NonNull ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Returns the task manager associated with the client.
     *
     * @return The task manager.
     */
    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Returns the current session, if available.
     *
     * @return The current session or null if no session exists.
     */
    @Nullable
    @Override
    public OperableSession getSession() {
        return session;
    }

    /**
     * Returns the list of server addresses.
     *
     * @return The list of addresses or null if not initialized.
     */
    @Nullable
    @Override
    public List<Address> getAddressList() {
        return addressList;
    }

    /**
     * Sets the list of server addresses.
     * If the client is currently initializing, the address list is set and initialization is
     * finished.
     *
     * @param addressList The list of addresses to be set.
     */
    @Override
    public void setAddressList(@NonNull List<Address> addressList) {
        if (addressList.isEmpty())
            throw new IllegalArgumentException("address list must not be empty");
        mainExecutor.execute(() -> {
            addressIndex = 0;
            this.addressList = addressList;
            if (isInitializing) {
                isInitializing = false;
                onStart();
            }
        });
    }

    /**
     * Starts the client. This method delegates to {@link #onStart(boolean)} with `true`.
     */
    @Override
    protected void onStart() {
        onStart(true);
    }

    /**
     * Starts the socket client.
     * <ul>
     *     <li>If the client is shut down (timestamp < 0), no action is taken.</li>
     *     <li>If the address list is not set, an initialization task is started.</li>
     *     <li>If a session exists, it is reused; otherwise, a new session is created and started.
     *     </li>
     * </ul>
     *
     * @param active Whether to reset the activation timestamp.
     */
    void onStart(boolean active) {
        if (isShutdown()) return;
        if (active) {
            state = STATE_ACTIVE;
            activeTimestamp = System.currentTimeMillis();
        }
        if (session == null && initialize()) {
            if (currentAddress == null) {
                currentAddress = addressList.get(addressIndex);
            }
            session = new SocketSession(this, currentAddress, addressIndex, sessionId++);
            session.open();
        }
    }

    /**
     * Initializes the client by setting up the address list and starting the initialization
     * process if necessary.
     * <p>
     * This method checks if the address list is already initialized. If not, it attempts to
     * retrieve the address list from the options. If the address list is still null, it marks the
     * client as initializing and starts the initialization process using the `ClientInitializer`.
     *
     * @return `true` if the address list is already initialized, `false` otherwise.
     */
    private boolean initialize() {
        if (addressList != null)
            return true; // Return true if the address list is already initialized.
        if (isInitializing) {
            logger.d("client is initializing, just wait for the result");
            return false;
        }
        // Attempt to retrieve the address list from the options.
        addressList = options.getAddressList();
        if (addressList == null) {
            // Mark the client as initializing.
            isInitializing = true;
            // Ensure the ClientInitializer is not null.
            requireNonNull(getClientInitializer())
                    .start(new InitializeEmitter()); // Start the initialization process.
            return false;
        }
        return true; // Return true if the address list is successfully initialized.
    }

    /**
     * Handles the successful initialization of the client.
     * <p>
     * This method is called when the initialization process completes successfully.
     * It updates the `addressList` with the provided server addresses, marks the
     * initialization process as complete, and starts the client.
     *
     * @param addressList The list of server addresses obtained during initialization.
     */
    private void onInitializeSuccess(List<Address> addressList) {
        if (isInitializing) {
            isInitializing = false;
            this.addressList = addressList;
            onStart();
        }
    }

    /**
     * Handles the failure of the client initialization process.
     * <p>
     * This method is called when the initialization process fails. It marks the
     * initialization process as complete and resets the task manager with the
     * provided exception.
     *
     * @param e The exception describing the reason for the initialization failure.
     */
    private void onInitializeFailure(EasyException e) {
        if (isInitializing) {
            isInitializing = false;
            taskManager.reset(e);
        }
    }

    /**
     * Stops the client. Closes the current session and resets the activation timestamp.
     */
    @Override
    protected void onStop() {
        if (isShutdown()) return;
        logger.i("stop socket client");
        state = STATE_SLEEP;
        if (session != null) {
            EasyException e = EasyException.create(ErrorCode.STOP, ErrorType.SYSTEM,
                    "socket client is stopped", session.getSuffix());
            session.close(e);
        }
    }

    /**
     * Shuts down the client. Closes the session and removes the client from the global registry.
     */
    @Override
    protected void onShutdown() {
        if (isShutdown()) return;
        logger.i("shutdown socket client");
        state = STATE_SHUTDOWN;
        EasyException e = EasyException.create(ErrorCode.SHUTDOWN, ErrorType.SYSTEM,
                "socket client on shutdown", suffix);
        if (session != null) {
            session.close(e);
        } else {
            taskManager.reset(e);
        }
        EasySocket.getInstance().removeSocketClient(this);
    }

    /**
     * Notifies listeners when a connection starts.
     *
     * @param session The session that started the connection.
     */
    @Override
    public void onConnecting(@NonNull Session session) {
        assert session == this.session;
        if (!connectionListeners.isEmpty()) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnecting(session);
                }
            });
        }
    }

    /**
     * Notifies listeners when a connection is successfully established.
     *
     * @param session The session that established the connection.
     */
    @Override
    public void onConnected(@NonNull final Session session) {
        assert session == this.session;
        if (!connectionListeners.isEmpty()) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnected(session);
                }
            });
        }
    }

    /**
     * Handles connection failures. Resets the session and schedules a reconnection.
     *
     * @param session The session that failed.
     * @param e       The exception that caused the failure.
     */
    @Override
    public void onConnectFailed(@NonNull final Session session, @NonNull EasyException e) {
        assert session == this.session;
        this.session = null;
        taskManager.reset(e);
        reconnectManager.delayedReconnect(session);

        if (!connectionListeners.isEmpty()) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectFailed(session, e);
                }
            });
        }
    }

    /**
     * Handles connection abortion. Resets the session and schedules a reconnection.
     *
     * @param session The session that was aborted.
     * @param e       The exception that caused the abortion.
     */
    @Override
    public void onDisconnected(@NonNull final Session session, @NonNull EasyException e) {
        assert session == this.session;
        this.session = null;
        taskManager.reset(e);
        reconnectManager.delayedReconnect(session);

        if (!connectionListeners.isEmpty()) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onDisconnected(session, e);
                }
            });
        }
    }

    /**
     * Notifies listeners when a connection becomes available.
     *
     * @param session The session that became available.
     */
    @Override
    public void onAvailable(@NonNull final Session session) {
        assert session == this.session;
        failedTimes = 0;
        taskManager.ready();

        if (!connectionListeners.isEmpty()) {
            callbackExecutor.execute(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onAvailable(session);
                }
            });
        }
    }

    /**
     * Starts the client asynchronously.
     */
    @Override
    public void start() {
        if (isShutdown()) return;
        mainExecutor.execute(this::onStart);
    }

    /**
     * Stops the client asynchronously.
     */
    @Override
    public void stop() {
        if (isShutdown()) return;
        mainExecutor.execute(this::onStop);
    }

    /**
     * Shuts down the client asynchronously.
     */
    @Override
    public void shutdown() {
        if (isShutdown()) return;
        mainExecutor.execute(this::onShutdown);
    }

    /**
     * Checks if the client is shut down.
     *
     * @return True if the client is shut down, false otherwise.
     */
    @Override
    public boolean isShutdown() {
        return state == STATE_SHUTDOWN;
    }

    /**
     * Checks if the client is connected.
     *
     * @return True if the client is connected, false otherwise.
     */
    @Override
    public boolean isConnected() {
        Session session = this.session;
        return session != null && session.isConnect();
    }

    /**
     * Checks if the client is available.
     *
     * @return True if the client is available, false otherwise.
     */
    @Override
    public boolean isAvailable() {
        Session session = this.session;
        return session != null && session.isAvailable();
    }

    /**
     * Handles network availability events. Attempts to reconnect if necessary.
     */
    @Override
    public void onNetworkAvailable() {
        if (state == STATE_ACTIVE && session == null) {
            reconnectManager.reconnect();
        }
    }

    /**
     * Switches to the next server in the address list if the failure threshold is reached.
     */
    void switchServer() {
        if (++failedTimes >= options.getRetryTimesPerAddress()) {
            failedTimes = 0;
            addressIndex = (addressIndex + 1) % addressList.size();
            currentAddress = addressList.get(addressIndex);
            logger.i("switch to server: " + currentAddress);
        }
    }

    /**
     * Checks if the client is active.
     *
     * @return True if the client is active, false otherwise.
     */
    boolean isActive() {
        if (state != STATE_ACTIVE) return false;

        long backgroundTimestamp = EasySocket.getInstance().getBackgroundTimestamp();
        if (backgroundTimestamp == 0) return true;

        long currentTimeMillis = System.currentTimeMillis();
        long backgroundActiveDurationInSec = options.getBackgroundActiveDurationInSec() * 1000L;
        return currentTimeMillis - backgroundTimestamp <= backgroundActiveDurationInSec
                && currentTimeMillis - activeTimestamp <= backgroundActiveDurationInSec;
    }

    /**
     * Returns a string representation of the client.
     *
     * @return A string containing the client's name.
     */
    @NonNull
    @Override
    public String toString() {
        return "EasySocketClient[" + "name=" + name + ']';
    }

    private class InitializeEmitter implements ClientInitializer.Emitter {
        /**
         * Called when initialization succeeds. Starts the client with the new address list.
         *
         * @param addressList The list of server addresses.
         */
        @Override
        public void postSuccess(@NonNull List<Address> addressList) {
            if (addressList.isEmpty()) {
                logger.e("address list is empty");
                EasyException e = EasyException.create(ErrorCode.INIT_FAILED, ErrorType.SYSTEM,
                        "address list is empty", suffix);
                mainExecutor.execute(() -> onInitializeFailure(e));
            } else {
                mainExecutor.execute(() -> onInitializeSuccess(addressList));
            }
        }

        /**
         * Called when initialization fails. Resets the task manager with the given exception.
         *
         * @param t The exception that caused the failure.
         */
        @Override
        public void postFailure(@NonNull Throwable t) {
            logger.e("socket client initialize failed, error: " + t.getMessage());
            EasyException e = EasyException.create(ErrorCode.INIT_FAILED, ErrorType.SYSTEM,
                    "socket client initialize failed", suffix, t);
            mainExecutor.execute(() -> onInitializeFailure(e));
        }
    }
}