package com.orientsec.easysocket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.client.ClientInitializer;
import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.session.SessionInitializer;
import com.orientsec.easysocket.utils.Executors;

import java.util.List;
import java.util.concurrent.Executor;

import javax.net.SocketFactory;

/**
 * Represents the configuration options for the EasySocket library.
 * This class provides various settings for socket connections, including
 * connection timeouts, heartbeat configurations, reconnection policies, and
 * thread executors for managing tasks.
 */
public class Options {

    /**
     * Indicates whether the application is in debug mode.
     */
    private final boolean debuggable;

    /**
     * The name of the socket client.
     */
    private final String name;

    /**
     * Provider for the heartbeat decoder.
     */
    private final Provider<Decoder<Boolean>> pulseDecoderProvider;

    /**
     * Provider for the heartbeat request.
     */
    private final Provider<Request<Boolean>> pulseRequestProvider;

    /**
     * Provider for the socket factory.
     */
    private final Provider<SocketFactory> socketFactoryProvider;

    /**
     * Provider for the data protocol parser.
     */
    private final Provider<HeadParser> headParserProvider;

    /**
     * Provider for the push message handler.
     */
    private final Provider<PushManager<?, ?>> pushManagerProvider;
    /**
     * Provider for the socket client initializer.
     */
    private final Provider<ClientInitializer> clientInitializerProvider;

    /**
     * Provider for the session initializer.
     */
    private final Provider<SessionInitializer> sessionInitializerProvider;

    /**
     * Executor for handling callback tasks, such as connection state changes
     * and request responses.
     */
    private final Executor callbackExecutor;

    /**
     * Executor for managing connection tasks, such as starting and stopping connections.
     */
    private final Executor connectExecutor;

    /**
     * Executor for encoding and decoding tasks.
     */
    private final Executor codecExecutor;

    /**
     * Executor for handling write operations.
     */
    private final Executor writeExecutor;

    /**
     * A list of server addresses to which the socket client can connect.
     * This list must not be empty.
     */
    private final List<Address> addressList;

    /**
     * The maximum size of data (in KB) that can be read to prevent memory overflow.
     */
    private final int maxReadSizeInKB;

    /**
     * The timeout duration (in seconds) for requests.
     */
    private final int requestTimeOutInMills;

    /**
     * The timeout duration (in seconds) for establishing a connection.
     */
    private final int connectTimeOutInMills;

    /**
     * The frequency (in seconds) of heartbeat messages.
     */
    private final int pulseIntervalInSec;

    /**
     * The number of consecutive heartbeat failures allowed before considering
     * the connection lost.
     */
    private final int pulseMaxLostTimes;

    /**
     * The duration (in seconds) the client remains active in the background.
     */
    private final int backgroundActiveDurationInSec;

    /**
     * The reconnection policy to be used when the connection is lost.
     */
    private final ReconnectPolicy reconnectPolicy;

    /**
     * The number of retry attempts for reconnection.
     */
    private final int retryTimes;

    /**
     * The interval (in milliseconds) between connection attempts.
     */
    private final int connectIntervalInMills;

    /**
     * A tag used to identify connection statistics.
     */
    private final int connectStatsTag;

    /**
     * A tag used to identify read operation statistics.
     */
    private final int readStatsTag;

    /**
     * A tag used to identify write operation statistics.
     */
    private final int writeStatsTag;

    /**
     * Constructs an `Options` instance using the provided builder.
     *
     * @param builder The builder containing the configuration settings.
     */
    private Options(Builder builder) {
        name = builder.name;
        debuggable = builder.debuggable;
        callbackExecutor = builder.callbackExecutor;
        connectExecutor = builder.connectExecutor;
        codecExecutor = builder.codecExecutor;
        writeExecutor = builder.writeExecutor;
        maxReadSizeInKB = builder.maxReadSizeInKB;
        requestTimeOutInMills = builder.requestTimeOutInMills;
        connectTimeOutInMills = builder.connectTimeOutInMills;
        pulseIntervalInSec = builder.pulseDurationInSec;
        pulseMaxLostTimes = builder.pulseMaxLostTimes;
        backgroundActiveDurationInSec = builder.backgroundActiveDurationInSec;
        reconnectPolicy = builder.reconnectPolicy;
        retryTimes = builder.retryTimes;
        connectIntervalInMills = builder.connectIntervalInMills;
        headParserProvider = builder.headParserProvider;
        clientInitializerProvider = builder.clientInitializerProvider;
        sessionInitializerProvider = builder.sessionInitializerProvider;
        pulseRequestProvider = builder.pulseRequestProvider;
        pulseDecoderProvider = builder.pulseDecoderProvider;
        pushManagerProvider = builder.pushManagerProvider;
        socketFactoryProvider = builder.socketFactoryProvider;
        connectStatsTag = builder.connectStatsTag;
        readStatsTag = builder.readStatsTag;
        writeStatsTag = builder.writeStatsTag;
        addressList = builder.addressList;
    }

    // Getter methods for accessing the configuration options...


    public boolean isDebuggable() {
        return debuggable;
    }

    public String getName() {
        return name;
    }

    @NonNull
    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    @NonNull
    public Executor getConnectExecutor() {
        return connectExecutor;
    }

    @NonNull
    public Executor getCodecExecutor() {
        return codecExecutor;
    }

    @NonNull
    public Executor getWriteExecutor() {
        return writeExecutor;
    }

    public int getMaxReadSizeInKB() {
        return maxReadSizeInKB;
    }

    public int getRequestTimeOutInMills() {
        return requestTimeOutInMills;
    }

    public int getPulseIntervalInSec() {
        return pulseIntervalInSec;
    }

    public int getPulseMaxLostTimes() {
        return pulseMaxLostTimes;
    }

    public int getBackgroundActiveDurationInSec() {
        return backgroundActiveDurationInSec;
    }

    @NonNull
    public ReconnectPolicy getReconnectPolicy() {
        return reconnectPolicy;
    }

    public int getConnectTimeOutInMills() {
        return connectTimeOutInMills;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getConnectIntervalInMills() {
        return connectIntervalInMills;
    }

    @Nullable
    public Provider<Request<Boolean>> getPulseRequestProvider() {
        return pulseRequestProvider;
    }

    @Nullable
    public Provider<Decoder<Boolean>> getPulseDecoderProvider() {
        return pulseDecoderProvider;
    }

    @NonNull
    public Provider<SocketFactory> getSocketFactoryProvider() {
        return socketFactoryProvider;
    }

    @NonNull
    public Provider<HeadParser> getHeadParserProvider() {
        return headParserProvider;
    }

    @Nullable
    public Provider<PushManager<?, ?>> getPushManagerProvider() {
        return pushManagerProvider;
    }

    @Nullable
    public Provider<ClientInitializer> getClientInitializerProvider() {
        return clientInitializerProvider;
    }

    @Nullable
    public Provider<SessionInitializer> getSessionInitializerProvider() {
        return sessionInitializerProvider;
    }

    public int getConnectStatsTag() {
        return connectStatsTag;
    }

    public int getReadStatsTag() {
        return readStatsTag;
    }

    public int getWriteStatsTag() {
        return writeStatsTag;
    }

    @Nullable
    public List<Address> getAddressList() {
        return addressList;
    }

    /**
     * Builder class for constructing `Options` instances.
     * Provides methods for setting various configuration parameters.
     */
    public static final class Builder {
        // The name of the socket client.
        private String name = "";
        // Indicates whether the application is in debug mode.
        private boolean debuggable;
        // Provider for the heartbeat request.
        private Provider<Request<Boolean>> pulseRequestProvider;
        // Provider for the heartbeat decoder.
        private Provider<Decoder<Boolean>> pulseDecoderProvider;
        // Provider for the socket factory.
        private Provider<SocketFactory> socketFactoryProvider;
        // Provider for the data protocol parser.
        private Provider<HeadParser> headParserProvider;
        // Provider for the push message handler.
        private Provider<PushManager<?, ?>> pushManagerProvider;
        // Provider for the socket client initializer.
        private Provider<ClientInitializer> clientInitializerProvider;
        // Provider for the session initializer.
        private Provider<SessionInitializer> sessionInitializerProvider;
        // Executor for handling callback tasks.
        private Executor callbackExecutor;
        // Executor for managing connection tasks.
        private Executor connectExecutor;
        // Executor for encoding and decoding tasks.
        private Executor codecExecutor;
        // Executor for handling write operations.
        private Executor writeExecutor;
        // A list of server addresses for the socket client.
        private List<Address> addressList;
        // Maximum size of data (in KB) that can be read.
        private int maxReadSizeInKB = 1024;
        // Timeout duration (in milliseconds) for requests.
        private int requestTimeOutInMills = 5000;
        // Timeout duration (in milliseconds) for connections.
        private int connectTimeOutInMills = 5000;
        // Frequency (in seconds) of heartbeat messages.
        private int pulseDurationInSec = 60;
        // Number of consecutive heartbeat failures allowed.
        private int pulseMaxLostTimes = 2;
        // Duration (in seconds) the client remains active in the background.
        private int backgroundActiveDurationInSec = 30;
        // Reconnection policy for lost connections.
        private ReconnectPolicy reconnectPolicy = ReconnectPolicy.ACTIVE;
        // Number of retry attempts for reconnection.
        private int retryTimes;
        // Interval (in milliseconds) between connection attempts.
        private int connectIntervalInMills = 3000;
        // Tag for connection statistics.
        private int connectStatsTag = 0x1001;
        // Tag for read operation statistics.
        private int readStatsTag = 0x1002;
        // Tag for write operation statistics.
        private int writeStatsTag = 0x1003;

        /**
         * Default constructor for the `Builder` class.
         */
        public Builder() {
        }

        /**
         * Sets the name of the socket client.
         *
         * @param val The name to set.
         * @return This builder instance for chaining.
         */
        public Builder name(@NonNull String val) {
            name = val;
            return this;
        }

        /**
         * Enables or disables debug mode.
         *
         * @param val `true` to enable debug mode, `false` to disable it.
         * @return This builder instance for chaining.
         */
        public Builder debuggable(boolean val) {
            debuggable = val;
            return this;
        }

        /**
         * Sets the provider for the heartbeat request.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder pulseRequestProvider(@NonNull Provider<Request<Boolean>> val) {
            pulseRequestProvider = val;
            return this;
        }

        /**
         * Sets the provider for the heartbeat decoder.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder pulseDecoderProvider(@NonNull Provider<Decoder<Boolean>> val) {
            pulseDecoderProvider = val;
            return this;
        }

        /**
         * Sets the provider for the data protocol parser.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder headParserProvider(@NonNull Provider<HeadParser> val) {
            headParserProvider = val;
            return this;
        }

        /**
         * Sets the provider for the push message handler.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder pushManagerProvider(@NonNull Provider<PushManager<?, ?>> val) {
            pushManagerProvider = val;
            return this;
        }

        /**
         * Sets the provider for the socket client initializer.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder clientInitializerProvider(@NonNull Provider<ClientInitializer> val) {
            clientInitializerProvider = val;
            return this;
        }

        /**
         * Sets the provider for the session initializer.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder sessionInitializerProvider(@NonNull Provider<SessionInitializer> val) {
            sessionInitializerProvider = val;
            return this;
        }

        /**
         * Sets the provider for the socket factory.
         *
         * @param val The provider to set.
         * @return This builder instance for chaining.
         */
        public Builder socketFactoryProvider(@NonNull Provider<SocketFactory> val) {
            socketFactoryProvider = val;
            return this;
        }

        /**
         * Sets the executor for handling callback tasks.
         *
         * @param val The executor to set.
         * @return This builder instance for chaining.
         */
        public Builder callbackExecutor(@NonNull Executor val) {
            callbackExecutor = val;
            return this;
        }

        /**
         * Sets the executor for managing connection tasks.
         *
         * @param val The executor to set.
         * @return This builder instance for chaining.
         */
        public Builder connectExecutor(@NonNull Executor val) {
            connectExecutor = val;
            return this;
        }

        /**
         * Sets the executor for encoding and decoding tasks.
         *
         * @param val The executor to set.
         * @return This builder instance for chaining.
         */
        public Builder codecExecutor(@NonNull Executor val) {
            codecExecutor = val;
            return this;
        }

        /**
         * Sets the executor for handling write operations.
         *
         * @param val The executor to set.
         * @return This builder instance for chaining.
         */
        public Builder writeExecutor(@NonNull Executor val) {
            writeExecutor = val;
            return this;
        }

        /**
         * Sets the list of server addresses for the socket client.
         * This list must not be empty; otherwise, an exception will be thrown.
         *
         * @param val The list of addresses to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the address list is empty.
         */
        public Builder addressList(@NonNull List<Address> val) {
            if (val.isEmpty()) {
                throw new IllegalArgumentException("Address list cannot be empty.");
            }
            addressList = val;
            return this;
        }

        /**
         * Sets the maximum size of data (in KB) that can be read.
         *
         * @param val The maximum size to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is not positive.
         */
        public Builder maxReadSizeKB(int val) {
            if (val <= 0) {
                throw new IllegalArgumentException("Max read data size in KB must be positive.");
            }
            maxReadSizeInKB = val;
            return this;
        }

        /**
         * Sets the timeout duration (in milliseconds) for requests.
         *
         * @param val The timeout duration to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is not positive.
         */
        public Builder requestTimeOutInMills(int val) {
            if (val <= 0) {
                throw new IllegalArgumentException("Request time out must be positive.");
            }
            requestTimeOutInMills = val;
            return this;
        }

        /**
         * Sets the timeout duration (in milliseconds) for connections.
         *
         * @param val The timeout duration to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is not positive.
         */
        public Builder connectTimeOutInMills(int val) {
            if (val <= 0) {
                throw new IllegalArgumentException("Connect time out must be positive.");
            }
            connectTimeOutInMills = val;
            return this;
        }

        /**
         * Sets the frequency (in seconds) of heartbeat messages.
         *
         * @param val The frequency to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is less than 30 seconds.
         */
        public Builder pulseDurationInSec(int val) {
            if (val < 30) {
                throw new IllegalArgumentException("Pulse rate must be at least 30 seconds.");
            }
            pulseDurationInSec = val;
            return this;
        }

        /**
         * Sets the number of consecutive heartbeat failures allowed.
         *
         * @param val The number of failures to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is negative.
         */
        public Builder pulseMaxLostTimes(int val) {
            if (val < 0) {
                throw new IllegalArgumentException("Pulse lost times cannot be negative.");
            }
            pulseMaxLostTimes = val;
            return this;
        }

        /**
         * Sets the duration (in seconds) the client remains active in the background.
         *
         * @param val The duration to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is negative.
         */
        public Builder backgroundActiveDurationInSec(int val) {
            if (val < 0) {
                throw new IllegalArgumentException("Live time must be positive.");
            }
            backgroundActiveDurationInSec = val;
            return this;
        }

        /**
         * Sets the reconnection policy for lost connections.
         *
         * @param val The reconnection policy to set.
         * @return This builder instance for chaining.
         */
        public Builder reconnectPolicy(@NonNull ReconnectPolicy val) {
            reconnectPolicy = val;
            return this;
        }

        /**
         * Sets the number of retry attempts for reconnection.
         *
         * @param val The number of attempts to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is negative.
         */
        public Builder retryTimes(int val) {
            if (val < 0) {
                throw new IllegalArgumentException("Retry times cannot be negative.");
            }
            retryTimes = val;
            return this;
        }

        /**
         * Sets the interval (in milliseconds) between connection attempts.
         *
         * @param val The interval to set.
         * @return This builder instance for chaining.
         * @throws IllegalArgumentException If the value is less than or equal to 1000 milliseconds.
         */
        public Builder connectIntervalInMills(int val) {
            if (val <= 1000) {
                throw new IllegalArgumentException("Connect interval must be greater than" +
                        " 1000 milliseconds.");
            }
            connectIntervalInMills = val;
            return this;
        }

        /**
         * Sets the tag for connection statistics.
         *
         * @param val The tag to set.
         * @return This builder instance for chaining.
         */
        public Builder connectStatsTag(int val) {
            connectStatsTag = val;
            return this;
        }

        /**
         * Sets the tag for read operation statistics.
         *
         * @param val The tag to set.
         * @return This builder instance for chaining.
         */
        public Builder readStatsTag(int val) {
            readStatsTag = val;
            return this;
        }

        /**
         * Sets the tag for write operation statistics.
         *
         * @param val The tag to set.
         * @return This builder instance for chaining.
         */
        public Builder writeStatsTag(int val) {
            writeStatsTag = val;
            return this;
        }

        /**
         * Builds and returns an `Options` instance with the configured settings.
         *
         * @return A new `Options` instance.
         * @throws IllegalArgumentException If any required parameter is invalid or missing.
         */
        @NonNull
        public Options build() {
            if (headParserProvider == null) {
                throw new IllegalArgumentException("Head parser provider has not been set.");
            }
            if (addressList == null && clientInitializerProvider == null) {
                throw new IllegalArgumentException("address list or client initializer" +
                        " provider should be set.");
            }
            if (socketFactoryProvider == null) {
                socketFactoryProvider = new DefaultSocketFactoryProvider();
            }
            if (callbackExecutor == null) {
                callbackExecutor = Executors.defaultMainThreadExecutor();
            }
            if (connectExecutor == null) {
                connectExecutor = Executors.defaultConnectExecutor();
            }
            if (codecExecutor == null) {
                codecExecutor = Executors.defaultCodecExecutor();
            }
            if (writeExecutor == null) {
                writeExecutor = Executors.defaultWriteExecutor();
            }
            return new Options(this);
        }

        @NonNull
        public SocketClient open() {
            return EasySocket.getInstance().open(build());
        }
    }
}
