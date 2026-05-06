package com.orientsec.easysocket.client;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.ConnectionListener;
import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Provider;
import com.orientsec.easysocket.SocketClient;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.session.OperableSession;
import com.orientsec.easysocket.session.SessionInitializer;
import com.orientsec.easysocket.task.TaskManager;
import com.orientsec.easysocket.utils.LogFactory;
import com.orientsec.easysocket.utils.Logger;

import javax.net.SocketFactory;

/**
 * BaseSocketClient is an abstract class that provides the base implementation for a socket client.
 * It defines the core structure and behavior for managing socket connections, tasks, and listeners.
 */
public abstract class BaseSocketClient implements SocketClient, ConnectionListener {

    protected final Options options; // Configuration options for the socket client.
    protected final EasyExecutor mainExecutor; // Executor for running client operations.
    private PushManager<?, ?> pushManager; // Manages push notifications.
    private HeadParser headParser; // Parses the header of socket messages.
    private ClientInitializer clientInitializer; // Initializes the socket client.
    private SessionInitializer sessionInitializer; // Initializes the session.
    private SocketFactory socketFactory; // Factory for creating socket instances.
    private Decoder<Boolean> pulseDecoder; // Decoder for heartbeat (pulse) messages.
    private Request<Boolean> pulseRequest; // Request object for sending heartbeat (pulse) messages.
    protected final Logger logger; // Logger for logging client activities.
    public final String suffix; // Suffix used for logging and identification.

    /**
     * Constructs a BaseSocketClient with the specified options and mainExecutor.
     *
     * @param options      Configuration options for the client.
     * @param mainExecutor Executor for running client operations.
     */
    public BaseSocketClient(Options options, EasyExecutor mainExecutor) {
        this.options = options;
        this.mainExecutor = mainExecutor;
        this.suffix = "  Client[" + options.getName() + "]";
        this.logger = LogFactory.getLogger(options, suffix);
    }

    /**
     * Starts the socket client. This method must be implemented by subclasses.
     * It is called on the main thread.
     */
    @MainThread
    protected abstract void onStart();

    /**
     * Stops the socket client. This method must be implemented by subclasses.
     * It is called on the main thread.
     */
    @MainThread
    protected abstract void onStop();

    /**
     * Shuts down the socket client. This method must be implemented by subclasses.
     * It is called on the main thread.
     */
    @MainThread
    protected abstract void onShutdown();

    /**
     * Handles network availability events. This method must be implemented by subclasses.
     * It is called on the main thread.
     */
    @MainThread
    public abstract void onNetworkAvailable();

    /**
     * Returns the task manager associated with the client.
     *
     * @return The task manager.
     */
    public abstract TaskManager getTaskManager();

    /**
     * Returns the push manager associated with the client.
     * If the push manager is not initialized, it will be created using the provider.
     *
     * @return The push manager or null if no provider is available.
     */
    @Nullable
    @Override
    public synchronized PushManager<?, ?> getPushManager() {
        if (pushManager == null) {
            Provider<PushManager<?, ?>> provider = options.getPushManagerProvider();
            if (provider != null) {
                pushManager = provider.get(this);
            }
        }
        return pushManager;
    }

    /**
     * Returns the head parser associated with the client.
     * If the head parser is not initialized, it will be created using the provider.
     *
     * @return The head parser.
     */
    @NonNull
    public synchronized HeadParser getHeadParser() {
        if (headParser == null) {
            headParser = options.getHeadParserProvider().get(this);
        }
        return headParser;
    }

    /**
     * Returns the socket factory associated with the client.
     * If the socket factory is not initialized, it will be created using the provider.
     *
     * @return The socket factory.
     */
    @NonNull
    public synchronized SocketFactory getSocketFactory() {
        if (socketFactory == null) {
            socketFactory = options.getSocketFactoryProvider().get(this);
        }
        return socketFactory;
    }

    /**
     * Returns the `ClientInitializer` instance associated with the client.
     * <p>
     * If the `ClientInitializer` is not already initialized, this method will
     * create a new instance using the provider specified in the client's options.
     * This ensures that the `ClientInitializer` is always available when needed.
     *
     * @return The `ClientInitializer` instance.
     */
    @Nullable
    public synchronized ClientInitializer getClientInitializer() {
        if (clientInitializer == null) {
            Provider<ClientInitializer> provider = options.getClientInitializerProvider();
            if (provider != null) {
                clientInitializer = provider.get(this);
            }
        }
        return clientInitializer;
    }

    /**
     * Returns the initializer associated with the client.
     * If the initializer is not initialized, it will be created using the provider.
     *
     * @return The initializer.
     */
    @Nullable
    public synchronized SessionInitializer getSessionInitializer() {
        if (sessionInitializer == null) {
            Provider<SessionInitializer> provider = options.getSessionInitializerProvider();
            if (provider != null) {
                sessionInitializer = provider.get(this);
            }
        }
        return sessionInitializer;
    }

    /**
     * Returns the decoder for heartbeat (pulse) messages.
     * If the decoder is not initialized, it will be created using the provider.
     *
     * @return The pulse decoder or null if no provider is available.
     */
    @Nullable
    public synchronized Decoder<Boolean> getPulseDecoder() {
        if (pulseDecoder == null) {
            Provider<Decoder<Boolean>> provider = options.getPulseDecoderProvider();
            if (provider != null) {
                pulseDecoder = provider.get(this);
            }
        }
        return pulseDecoder;
    }

    /**
     * Returns the request object for sending heartbeat (pulse) messages.
     * If the request object is not initialized, it will be created using the provider.
     *
     * @return The pulse request or null if no provider is available.
     */
    @Nullable
    public synchronized Request<Boolean> getPulseRequest() {
        if (pulseRequest == null) {
            Provider<Request<Boolean>> provider = options.getPulseRequestProvider();
            if (provider != null) {
                pulseRequest = provider.get(this);
            }
        }
        return pulseRequest;
    }

    /**
     * Returns the configuration options for the client.
     *
     * @return The options.
     */
    @Override
    @NonNull
    public Options getOptions() {
        return options;
    }

    /**
     * Returns the logger associated with the client.
     *
     * @return The logger.
     */
    @Override
    @NonNull
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the main executor for the client.
     *
     * @return The main executor .
     */
    @NonNull
    public EasyExecutor getMainExecutor() {
        return mainExecutor;
    }

    /**
     * Returns the current session associated with the client.
     * This method must be implemented by subclasses.
     *
     * @return The current session or null if no session exists.
     */
    @Nullable
    @Override
    public abstract OperableSession getSession();
}