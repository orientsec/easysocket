package com.orientsec.easysocket

import android.util.Log
import com.orientsec.easysocket.client.ClientInitializer
import com.orientsec.easysocket.push.PushManager
import com.orientsec.easysocket.request.Decoder
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.SessionInitializer
import com.orientsec.easysocket.utils.Executors
import java.util.concurrent.Executor
import javax.net.SocketFactory

/**
 * Represents the configuration options for the EasySocket library.
 * This class provides various settings for socket connections, including
 * connection timeouts, heartbeat configurations, reconnection policies, and
 * coroutine settings.
 */
class Options private constructor(
    val name: String,
    val isDebuggable: Boolean,
    val minLogLevel: Int,
    val pulseRequestProvider: Provider<Request<Boolean>>?,
    val pulseDecoderProvider: Provider<Decoder<Boolean>>?,
    val socketFactoryProvider: Provider<SocketFactory>,
    val headParserProvider: Provider<HeadParser>,
    val pushManagerProvider: Provider<PushManager<*, *>>?,
    val clientInitializerProvider: Provider<ClientInitializer>?,
    val sessionInitializerProvider: Provider<SessionInitializer>?,
    /**
     * Executor for handling legacy callback tasks. 
     * If not provided, the default main thread executor will be used.
     */
    val callbackExecutor: Executor,
    val addressList: List<Address>?,
    val maxReadSizeKb: Int,
    val requestTimeoutMillis: Int,
    val connectTimeoutMillis: Int,
    val pulseIntervalSeconds: Int,
    val pulseMaxLostTimes: Int,
    val backgroundActiveDurationSeconds: Int,
    val reconnectPolicy: ReconnectPolicy,
    val retryTimesPerAddress: Int,
    val connectIntervalMillis: Int,
    val connectStatsTag: Int,
    val readStatsTag: Int,
    val writeStatsTag: Int,
    val taskRetryTimes: Int
) {

    companion object {
        /**
         * DSL function for building [Options].
         */
        inline fun build(block: Builder.() -> Unit): Options = Builder().apply(block).build()
    }

    class Builder {
        var name: String = ""
        var isDebuggable: Boolean = false
        var minLogLevel: Int = Log.INFO
            set(value) {
                require(value >= 0) { "Minimum log level must be non-negative." }
                field = value
            }

        var pulseRequestProvider: Provider<Request<Boolean>>? = null
        var pulseDecoderProvider: Provider<Decoder<Boolean>>? = null
        var socketFactoryProvider: Provider<SocketFactory>? = null
        var headParserProvider: Provider<HeadParser>? = null
        var pushManagerProvider: Provider<PushManager<*, *>>? = null
        var clientInitializerProvider: Provider<ClientInitializer>? = null
        var sessionInitializerProvider: Provider<SessionInitializer>? = null

        var callbackExecutor: Executor? = null

        var addressList: List<Address>? = null
            set(value) {
                require(value == null || value.isNotEmpty()) { "Address list cannot be empty." }
                field = value
            }

        var maxReadSizeKb: Int = 1024
            set(value) {
                require(value > 0) { "Max read data size in KB must be positive." }
                field = value
            }

        var requestTimeoutMillis: Int = 5000
            set(value) {
                require(value > 0) { "Request time out must be positive." }
                field = value
            }

        var connectTimeoutMillis: Int = 5000
            set(value) {
                require(value > 0) { "Connect time out must be positive." }
                field = value
            }

        var pulseIntervalSeconds: Int = 60
            set(value) {
                require(value >= 30) { "Pulse rate must be at least 30 seconds." }
                field = value
            }

        var pulseMaxLostTimes: Int = 2
            set(value) {
                require(value >= 0) { "Pulse lost times cannot be negative." }
                field = value
            }

        var backgroundActiveDurationSeconds: Int = 30
            set(value) {
                require(value >= 0) { "Live time must be positive." }
                field = value
            }

        var reconnectPolicy: ReconnectPolicy = ReconnectPolicy.ACTIVE
        var retryTimesPerAddress: Int = 0
            set(value) {
                require(value >= 0) { "Retry times cannot be negative." }
                field = value
            }

        var connectIntervalMillis: Int = 3000
            set(value) {
                require(value > 1000) { "Connect interval must be greater than 1000 milliseconds." }
                field = value
            }

        var connectStatsTag: Int = 0x1001
        var readStatsTag: Int = 0x1002
        var writeStatsTag: Int = 0x1003
        var taskRetryTimes: Int = 2
            set(value) {
                require(value >= 0) { "Task retry times cannot be negative." }
                field = value
            }

        /**
         * Builds and returns an [Options] instance with the configured settings.
         */
        fun build(): Options {
            val headParser = headParserProvider
                ?: throw IllegalArgumentException("Head parser provider has not been set.")
            if (addressList == null && clientInitializerProvider == null) {
                throw IllegalArgumentException(
                    "address list or client initializer provider " +
                            "should be set."
                )
            }

            return Options(
                name = name,
                isDebuggable = isDebuggable,
                minLogLevel = minLogLevel,
                pulseRequestProvider = pulseRequestProvider,
                pulseDecoderProvider = pulseDecoderProvider,
                socketFactoryProvider = socketFactoryProvider
                    ?: { SocketFactory.getDefault() },
                headParserProvider = headParser,
                pushManagerProvider = pushManagerProvider,
                clientInitializerProvider = clientInitializerProvider,
                sessionInitializerProvider = sessionInitializerProvider,
                callbackExecutor = callbackExecutor ?: Executors.defaultMainThreadExecutor(),
                addressList = addressList,
                maxReadSizeKb = maxReadSizeKb,
                requestTimeoutMillis = requestTimeoutMillis,
                connectTimeoutMillis = connectTimeoutMillis,
                pulseIntervalSeconds = pulseIntervalSeconds,
                pulseMaxLostTimes = pulseMaxLostTimes,
                backgroundActiveDurationSeconds = backgroundActiveDurationSeconds,
                reconnectPolicy = reconnectPolicy,
                retryTimesPerAddress = retryTimesPerAddress,
                connectIntervalMillis = connectIntervalMillis,
                connectStatsTag = connectStatsTag,
                readStatsTag = readStatsTag,
                writeStatsTag = writeStatsTag,
                taskRetryTimes = taskRetryTimes
            )
        }

        /**
         * Builds the options and opens a new [SocketClient].
         */
        fun open(): SocketClient {
            return EasySocket.open(build())
        }
    }
}

/**
 * The `Provider` interface defines a generic contract for providing instances of a specific type.
 * Implementations of this interface are responsible for supplying objects, typically based on
 * the provided `SocketClient` instance.
 *
 * @param T The type of object that this provider supplies.
 */
typealias Provider<T> = (SocketClient) -> T