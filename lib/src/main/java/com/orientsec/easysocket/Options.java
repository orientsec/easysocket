package com.orientsec.easysocket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.push.PushManager;
import com.orientsec.easysocket.request.Decoder;
import com.orientsec.easysocket.request.Request;
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
    private final boolean debug;

    /**
     * The name of the socket client.
     */
    private final String name;

    /**
     * Indicates whether detailed logging is enabled.
     */
    private final boolean detailLog;

    /**
     * Provider for the heartbeat decoder.
     */
    private final Provider<Decoder<Boolean>> pulseDecoderProvider;

    /**
     * Provider for the heartbeat request.
     */
    private final Provider<Request<Boolean>> pulseRequestProvider;

    /**
     * Provider for the list of server addresses.
     */
    private final Provider<List<Address>> addressProvider;

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
     * Provider for the session initializer.
     */
    private final Provider<SessionInitializer> initializerProvider;

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
     * The maximum size of data (in KB) that can be read to prevent memory overflow.
     */
    private final int maxReadDataKB;

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
    private final int pulseRate;

    /**
     * The number of consecutive heartbeat failures allowed before considering
     * the connection lost.
     */
    private final int pulseLostTimes;

    /**
     * The duration (in seconds) the client remains active in the background.
     */
    private final int liveTime;

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
        debug = builder.debug;
        callbackExecutor = builder.callbackExecutor;
        connectExecutor = builder.connectExecutor;
        codecExecutor = builder.codecExecutor;
        writeExecutor = builder.writeExecutor;
        maxReadDataKB = builder.maxReadDataKB;
        requestTimeOutInMills = builder.requestTimeOutInMills;
        connectTimeOutInMills = builder.connectTimeOutInMills;
        pulseRate = builder.pulseRate;
        pulseLostTimes = builder.pulseLostTimes;
        liveTime = builder.liveTime;
        reconnectPolicy = builder.reconnectPolicy;
        retryTimes = builder.retryTimes;
        connectIntervalInMills = builder.connectIntervalInMills;
        addressProvider = builder.addressProvider;
        headParserProvider = builder.headParserProvider;
        initializerProvider = builder.initializerProvider;
        pulseRequestProvider = builder.pulseRequestProvider;
        pulseDecoderProvider = builder.pulseDecoderProvider;
        pushManagerProvider = builder.pushManagerProvider;
        socketFactoryProvider = builder.socketFactoryProvider;
        detailLog = builder.detailLog;
        connectStatsTag = builder.connectStatsTag;
        readStatsTag = builder.readStatsTag;
        writeStatsTag = builder.writeStatsTag;
    }

    // Getter methods for accessing the configuration options...


    public boolean isDebug() {
        return debug;
    }

    public String getName() {
        return name;
    }

    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    public Executor getConnectExecutor() {
        return connectExecutor;
    }

    public Executor getCodecExecutor() {
        return codecExecutor;
    }

    public Executor getWriteExecutor() {
        return writeExecutor;
    }

    public int getMaxReadDataKB() {
        return maxReadDataKB;
    }

    public int getRequestTimeOutInMills() {
        return requestTimeOutInMills;
    }

    public int getPulseRate() {
        return pulseRate;
    }

    public int getPulseLostTimes() {
        return pulseLostTimes;
    }

    public int getLiveTime() {
        return liveTime;
    }

    public ReconnectPolicy getLivePolicy() {
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
    public Provider<List<Address>> getAddressProvider() {
        return addressProvider;
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

    @NonNull
    public Provider<SessionInitializer> getInitializerProvider() {
        return initializerProvider;
    }

    public boolean isDetailLog() {
        return detailLog;
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

    /**
     * Builder class for constructing `Options` instances.
     * Provides methods for setting various configuration parameters.
     */
    public static final class Builder {
        private String name = "";
        private boolean debug;
        private Provider<Request<Boolean>> pulseRequestProvider;
        private Provider<Decoder<Boolean>> pulseDecoderProvider;
        private Provider<List<Address>> addressProvider;
        private Provider<SocketFactory> socketFactoryProvider;
        private Provider<HeadParser> headParserProvider;
        private Provider<PushManager<?, ?>> pushManagerProvider;
        private Provider<SessionInitializer> initializerProvider;
        private Executor callbackExecutor;
        private Executor connectExecutor;
        private Executor codecExecutor;
        private Executor writeExecutor;
        private int maxReadDataKB = 1024;
        private int requestTimeOutInMills = 5000;
        private int connectTimeOutInMills = 5000;
        private int pulseRate = 60;
        private int pulseLostTimes = 2;
        private int liveTime = 30;
        private ReconnectPolicy reconnectPolicy = ReconnectPolicy.ACTIVE;
        private int retryTimes;
        private int connectIntervalInMills = 3000;
        private boolean detailLog = true;
        private int connectStatsTag = 0x1001;
        private int readStatsTag = 0x1002;
        private int writeStatsTag = 0x1003;

        public Builder() {
        }

        public Builder name(@NonNull String val) {
            name = val;
            return this;
        }

        public Builder debug(boolean val) {
            debug = val;
            return this;
        }

        public Builder pulseRequestProvider(@NonNull Provider<Request<Boolean>> val) {
            pulseRequestProvider = val;
            return this;
        }

        public Builder pulseDecoderProvider(@NonNull Provider<Decoder<Boolean>> val) {
            pulseDecoderProvider = val;
            return this;
        }

        public Builder addressList(@NonNull List<Address> val) {
            addressProvider = StaticAddressProvider.build(val);
            return this;
        }

        public Builder addressProvider(@NonNull Provider<List<Address>> val) {
            addressProvider = val;
            return this;
        }

        public Builder headParserProvider(@NonNull Provider<HeadParser> val) {
            headParserProvider = val;
            return this;
        }

        public Builder pushManagerProvider(@NonNull Provider<PushManager<?, ?>> val) {
            pushManagerProvider = val;
            return this;
        }

        public Builder initializerProvider(@NonNull Provider<SessionInitializer> val) {
            initializerProvider = val;
            return this;
        }

        public Builder socketFactoryProvider(@NonNull Provider<SocketFactory> val) {
            socketFactoryProvider = val;
            return this;
        }

        public Builder callbackExecutor(@NonNull Executor val) {
            callbackExecutor = val;
            return this;
        }

        public Builder connectExecutor(@NonNull Executor val) {
            connectExecutor = val;
            return this;
        }

        public Builder codecExecutor(@NonNull Executor val) {
            codecExecutor = val;
            return this;
        }

        public Builder writeExecutor(@NonNull Executor val) {
            writeExecutor = val;
            return this;
        }

        public Builder maxReadDataKB(int val) {
            maxReadDataKB = val;
            return this;
        }

        public Builder requestTimeOutInMills(int val) {
            requestTimeOutInMills = val;
            return this;
        }

        public Builder connectTimeOutInMills(int val) {
            connectTimeOutInMills = val;
            return this;
        }

        public Builder pulseRate(int val) {
            pulseRate = val;
            return this;
        }

        public Builder pulseLostTimes(int val) {
            pulseLostTimes = val;
            return this;
        }

        public Builder liveTime(int val) {
            liveTime = val;
            return this;
        }

        public Builder livePolicy(@NonNull ReconnectPolicy val) {
            reconnectPolicy = val;
            return this;
        }

        public Builder retryTimes(int val) {
            retryTimes = val;
            return this;
        }

        public Builder connectIntervalInMills(int val) {
            connectIntervalInMills = val;
            return this;
        }

        public Builder detailLog(boolean val) {
            detailLog = val;
            return this;
        }

        public Builder connectStatsTag(int val) {
            connectStatsTag = val;
            return this;
        }

        public Builder readStatsTag(int val) {
            readStatsTag = val;
            return this;
        }

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
            String error = checkParams();
            if (error.isEmpty()) {
                return new Options(this);
            }
            throw new IllegalArgumentException(error);
        }

        @NonNull
        public SocketClient open() {
            return EasySocket.getInstance().open(build());
        }

        private String checkParams() {
            if (headParserProvider == null) {
                return "Head parser provider not set.";
            }
            if (addressProvider == null) {
                return "Address provider not set.";
            }
            if (maxReadDataKB <= 0) {
                return "Max read data size in kb must be positive.";
            }
            if (connectTimeOutInMills < 0) {
                return "Connect time out is negative.";
            }
            if (requestTimeOutInMills <= 0) {
                return "Request time out must be positive..";
            }
            if (pulseRate < 30) {
                return "Pulse rate must big than 30s.";
            }
            if (pulseLostTimes < 0) {
                return "Pulse lost time is negative.";
            }
            if (liveTime < 0) {
                return "Live time must be positive.";
            }
            if (retryTimes < 0) {
                return "Retry time is negative.";
            }
            if (connectIntervalInMills <= 1000) {
                return "Connect interval must big than 1000ms.";
            }
            if (socketFactoryProvider == null) {
                socketFactoryProvider = new DefaultSocketFactoryProvider();
            }
            if (initializerProvider == null) {
                initializerProvider = new DefaultInitializerProvider();
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

            return "";
        }
    }
}
