package com.orientsec.easysocket

import com.orientsec.easysocket.client.ClientInitializer
import com.orientsec.easysocket.push.PushManager
import com.orientsec.easysocket.request.Decoder
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.SessionFactory
import com.orientsec.easysocket.session.SessionInitializer
import com.orientsec.easysocket.session.ktor.KtorSessionFactory
import com.orientsec.easysocket.utils.NoTrafficProfiler
import com.orientsec.easysocket.utils.Platform
import com.orientsec.easysocket.utils.TrafficProfiler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * EasySocket 库的配置选项类。
 *
 * 包含 Socket 连接的各种设置，如连接超时、心跳配置、重连策略、
 * 编解码调度器、回调调度器等。通过 [Builder] 构建，支持 DSL 风格配置。
 *
 * 使用示例：
 * ```kotlin
 * val options = Options.build {
 *     name = "my-socket"
 *     isDebuggable = true
 *     useAndroidDefaults()
 *     headParserProvider = { MyHeadParser() }
 *     addressList = listOf(Address("192.168.1.1", 8080))
 *     pulseIntervalSeconds = 60
 *     reconnectPolicy = ReconnectPolicy.ALWAYS
 * }
 * ```
 */
class Options private constructor(
    /** 连接名称，用于日志标识 */
    val name: String,
    /** 是否启用调试模式，调试模式下会输出详细日志 */
    val isDebuggable: Boolean,
    /** 最低日志输出级别，低于此级别的日志将被忽略 */
    val minLogLevel: Int,
    /** 心跳请求提供者，返回心跳请求对象 */
    val pulseRequestProvider: Provider<Request<Boolean>>?,
    /** 心跳响应解码器提供者，用于解码心跳响应 */
    val pulseDecoderProvider: Provider<Decoder<Boolean>>?,
    /** 协议头解析器提供者，必须设置 */
    val headParserProvider: Provider<HeadParser>,
    /** 推送消息管理器提供者，可选 */
    val pushManagerProvider: Provider<PushManager<*, *>>?,
    /** 客户端初始化器提供者，用于动态获取服务器地址列表 */
    val clientInitializerProvider: Provider<ClientInitializer>?,
    /** 会话初始化器提供者，用于连接成功后的登录等操作 */
    val sessionInitializerProvider: Provider<SessionInitializer>?,
    /** 会话工厂，负责创建具体的连接会话 */
    val sessionFactory: SessionFactory,
    /** 流量统计器，用于标记 Socket 流量 */
    val trafficProfiler: TrafficProfiler,
    /** 编解码调度器，用于执行数据的编码和解码操作 */
    val codecDispatcher: CoroutineDispatcher,
    /** 回调调度器，用于执行用户回调，默认为主线程 */
    val callbackDispatcher: CoroutineDispatcher,
    /** 服务器地址列表，与 [clientInitializerProvider] 二选一 */
    val addressList: List<Address>?,
    /** 单次读取最大数据量（KB），防止读取超大包导致 OOM */
    val maxReadSizeKb: Int,
    /** 请求超时时间（毫秒），发送成功后等待响应的最大时间 */
    val requestTimeoutMills: Int,
    /** 连接超时时间（毫秒） */
    val connectTimeoutMills: Int,
    /** Tls超时时间（毫秒） */
    val tlsTimeoutMills: Int,
    /** 心跳间隔时间（秒），最小 30 秒 */
    val pulseIntervalSeconds: Int,
    /** 心跳最大丢失次数，超过后认为连接断开 */
    val pulseMaxLostTimes: Int,
    /** 后台活跃持续时间（秒），应用进入后台后保持连接的时间 */
    val backgroundActiveDurationSeconds: Int,
    /** 重连策略，控制是否及何时自动重连 */
    val reconnectPolicy: ReconnectPolicy,
    /** 每个地址的重试次数，超过后切换到下一个地址 */
    val retryTimesPerAddress: Int,
    /** 重连间隔时间（毫秒），两次连接尝试之间的最小间隔 */
    val connectIntervalMillis: Int,
    /** 连接操作的流量统计标签 */
    val connectStatsTag: Int,
    /** 读取操作的流量统计标签 */
    val readStatsTag: Int,
    /** 写入操作的流量统计标签 */
    val writeStatsTag: Int,
    /** 任务重试次数，连接不可用时任务的最大重试次数 */
    val taskRetryTimes: Int
) {

    companion object {
        /**
         * DSL 函数，用于构建 [Options] 实例。
         *
         * @param block 配置 lambda
         * @return 构建完成的 [Options] 实例
         */
        inline fun build(block: Builder.() -> Unit): Options = Builder().apply(block).build()
    }

    /**
     * Options 的构建器，提供 DSL 风格的配置方式。
     * 所有配置项都有合理的默认值，只需设置必要的项即可。
     */
    class Builder {
        /** 连接名称，用于日志标识 */
        var name: String = ""

        /** 是否启用调试模式 */
        var isDebuggable: Boolean = false

        /** 最低日志输出级别，默认 INFO */
        var minLogLevel: Int = Platform.LogLevel.INFO
            set(value) {
                require(value >= 0) { "Minimum log level must be non-negative." }
                field = value
            }

        /** 心跳请求提供者 */
        var pulseRequestProvider: Provider<Request<Boolean>>? = null

        /** 心跳响应解码器提供者 */
        var pulseDecoderProvider: Provider<Decoder<Boolean>>? = null

        /** 协议头解析器提供者，必须设置 */
        var headParserProvider: Provider<HeadParser>? = null

        /** 推送消息管理器提供者 */
        var pushManagerProvider: Provider<PushManager<*, *>>? = null

        /** 客户端初始化器提供者 */
        var clientInitializerProvider: Provider<ClientInitializer>? = null

        /** 会话初始化器提供者 */
        var sessionInitializerProvider: Provider<SessionInitializer>? = null

        /** 会话工厂，必须设置 */
        var sessionFactory: SessionFactory? = null

        /** 流量统计器，默认为空实现 */
        var trafficProfiler: TrafficProfiler = NoTrafficProfiler

        /** 编解码调度器，默认使用 Dispatchers.Default */
        var codecDispatcher: CoroutineDispatcher = Dispatchers.Default

        /** 回调调度器，默认使用平台主线程调度器 */
        var callbackDispatcher: CoroutineDispatcher = Platform.mainDispatcher

        /** 服务器地址列表 */
        var addressList: List<Address>? = null
            set(value) {
                require(value == null || value.isNotEmpty()) { "Address list cannot be empty." }
                field = value
            }

        /** 单次读取最大数据量（KB），默认 1024KB */
        var maxReadSizeKb: Int = 1024
            set(value) {
                require(value > 0) { "Max read data size in KB must be positive." }
                field = value
            }

        /** 请求超时时间（毫秒），默认 5000ms */
        var requestTimeoutMills: Int = 5000
            set(value) {
                require(value > 0) { "Request time out must be positive." }
                field = value
            }

        /** 连接超时时间（毫秒），默认 5000ms */
        var connectTimeoutMills: Int = 5000
            set(value) {
                require(value > 0) { "Connect time out must be positive." }
                field = value
            }

        /** 连接超时时间（毫秒），默认 5000ms */
        var tlsTimeoutMillis: Int = 5000
            set(value) {
                require(value > 0) { "TLS time out must be positive." }
                field = value
            }

        /** 心跳间隔时间（秒），默认 60 秒，最小 30 秒 */
        var pulseIntervalSeconds: Int = 60
            set(value) {
                require(value >= 30) { "Pulse rate must be at least 30 seconds." }
                field = value
            }

        /** 心跳最大丢失次数，默认 2 次 */
        var pulseMaxLostTimes: Int = 2
            set(value) {
                require(value >= 0) { "Pulse lost times cannot be negative." }
                field = value
            }

        /** 后台活跃持续时间（秒），默认 30 秒 */
        var backgroundActiveDurationSeconds: Int = 30
            set(value) {
                require(value >= 0) { "Live time must be positive." }
                field = value
            }

        /** 重连策略，默认 ACTIVE（仅前台重连） */
        var reconnectPolicy: ReconnectPolicy = ReconnectPolicy.ACTIVE

        /** 每个地址的重试次数，默认 0（不重试直接切换） */
        var retryTimesPerAddress: Int = 0
            set(value) {
                require(value >= 0) { "Retry times cannot be negative." }
                field = value
            }

        /** 重连间隔时间（毫秒），默认 3000ms，最小 1000ms */
        var connectIntervalMillis: Int = 3000
            set(value) {
                require(value > 1000) { "Connect interval must be greater than 1000 milliseconds." }
                field = value
            }

        /** 连接操作的流量统计标签 */
        var connectStatsTag: Int = 0x1001

        /** 读取操作的流量统计标签 */
        var readStatsTag: Int = 0x1002

        /** 写入操作的流量统计标签 */
        var writeStatsTag: Int = 0x1003

        /** 任务重试次数，默认 2 次 */
        var taskRetryTimes: Int = 2
            set(value) {
                require(value >= 0) { "Task retry times cannot be negative." }
                field = value
            }

        /**
         * 构建并返回 [Options] 实例。
         *
         * @throws IllegalArgumentException 如果缺少必要配置（headParserProvider、sessionFactory、addressList 或 clientInitializerProvider）
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
                headParserProvider = headParser,
                pushManagerProvider = pushManagerProvider,
                clientInitializerProvider = clientInitializerProvider,
                sessionInitializerProvider = sessionInitializerProvider,
                sessionFactory = sessionFactory
                    ?: throw IllegalArgumentException("Session factory must be set."),
                trafficProfiler = trafficProfiler,
                codecDispatcher = codecDispatcher,
                callbackDispatcher = callbackDispatcher,
                addressList = addressList,
                maxReadSizeKb = maxReadSizeKb,
                requestTimeoutMills = requestTimeoutMills,
                connectTimeoutMills = connectTimeoutMills,
                tlsTimeoutMills = tlsTimeoutMillis,
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
         * 配置使用 Ktor 作为底层网络引擎。
         * 自动设置 [KtorSessionFactory] 作为会话工厂。
         *
         * @return 当前 Builder 实例，支持链式调用
         */
        fun useKtorDefaults(): Builder {
            this.sessionFactory = KtorSessionFactory()
            return this
        }

        /**
         * 构建 Options 并直接打开一个新的 [SocketClient]。
         *
         * @return 新创建的 [SocketClient] 实例
         */
        fun open(): SocketClient {
            return EasySocket.open(build())
        }
    }
}

/**
 * 提供者接口，根据 [SocketClient] 实例创建指定类型的对象。
 * 用于延迟创建依赖 SocketClient 的组件（如 HeadParser、PushManager 等）。
 *
 * @param T 提供的对象类型
 */
typealias Provider<T> = (SocketClient) -> T