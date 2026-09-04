package com.orientsec.easysocket.session

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
import com.orientsec.easysocket.task.Callback
import com.orientsec.easysocket.task.Task
import com.orientsec.easysocket.task.TaskImpl
import com.orientsec.easysocket.task.TaskManager
import com.orientsec.easysocket.task.TaskType
import com.orientsec.easysocket.utils.LogFactory
import com.orientsec.easysocket.utils.Logger
import com.orientsec.easysocket.utils.Platform
import kotlinx.coroutines.launch

/**
 * 会话的抽象基类，封装了状态管理、数据包分发和生命周期逻辑。
 *
 * 管理一个 Socket 连接从建立到断开的完整生命周期，包括：
 * - 连接状态管理（IDLE -> STARTING -> CONNECTED -> AVAILABLE -> DETACHED）
 * - 数据包的分发（根据类型分发给任务管理器、心跳管理器或推送管理器）
 * - 心跳机制的启停
 * - 会话初始化（如登录）
 * - 连接时间统计
 *
 * 子类需要实现具体的连接逻辑（[performConnect]）、
 * 读取器/写入器的创建（[createReader]/[createWriter]）以及 Socket 关闭（[closeSocket]）。
 *
 * @param socketClient 所属的 Socket 客户端
 * @param address 连接的服务器地址
 * @param addressIndex 地址在列表中的索引
 * @param id 会话唯一标识
 */
abstract class AbstractSession(
    protected val socketClient: BaseSocketClient,
    override val address: Address,
    override val addressIndex: Int,
    protected val id: Long
) : OperableSession {

    /** 日志后缀，包含会话ID、地址和客户端名称，便于日志追踪 */
    override val suffix: String = ("  session(" + id + ")[" + address.host + ":"
            + address.port + "]  client[" + socketClient.options.name + "]")

    /** 配置选项 */
    protected val options: Options = socketClient.options

    /** 日志记录器 */
    override val logger: Logger = LogFactory.getLogger(options, suffix)

    /** 任务管理器 */
    protected val taskManager: TaskManager = socketClient.getTaskManager()

    /** 当前会话状态 */
    protected var state = State.IDLE

    /** 服务器是否可用（曾成功进入 AVAILABLE 状态） */
    protected var serverAvailable = false

    /** 心跳管理器 */
    protected var pulse: Pulse? = null

    /** 读取器 */
    protected var reader: Reader? = null

    /** 写入器 */
    override var writer: Writer? = null
        protected set

    /** 数据包处理器映射，按 [PacketType] 分发 */
    protected val messageHandlerMap: MutableMap<PacketType, PacketHandler> = mutableMapOf()

    /** 各阶段连接耗时统计 */
    protected val connectTimeMap: MutableMap<Period, Long> = mutableMapOf()

    /** 是否已连接（CONNECTED 或 AVAILABLE 状态） */
    override val isConnect: Boolean get() = state == State.CONNECTED || state == State.AVAILABLE

    /** 是否可用（AVAILABLE 状态） */
    override val isAvailable: Boolean get() = state == State.AVAILABLE

    /** 服务器是否可用 */
    override val isServerAvailable: Boolean get() = serverAvailable

    /**
     * 构建初始化任务。
     * 初始化任务在连接成功后立即执行，不需要等待连接可用。
     *
     * @param T 响应数据类型
     * @param request 请求对象
     * @param callback 回调接口
     * @return 初始化任务实例
     */
    override fun <T> buildTask(request: Request<T>, callback: Callback<T>): Task<T> {
        return TaskImpl(
            TaskType.INITIALIZE, taskManager.generateTaskId(),
            request, callback, socketClient, this
        )
    }

    /**
     * 处理接收到的数据包。
     * 将数据包分发到协程中异步处理。
     *
     * @param packet 接收到的数据包
     */
    override fun handlePacket(packet: Packet) {
        socketClient.scope.launch { onPacket(packet) }
    }

    /**
     * 异步处理数据包。
     * 根据数据包类型查找对应的处理器进行分发。
     * 如果会话已断开，则忽略该数据包。
     * 每次收到数据都会喂狗，重置心跳计时器。
     *
     * @param packet 接收到的数据包
     */
    private fun onPacket(packet: Packet) {
        if (state == State.DETACHED) return
        pulse?.feed()
        val packetHandler = messageHandlerMap[packet.type]
        if (packetHandler == null) {
            logger.w("no packet handler for " + packet.type)
        } else {
            logger.d("receive a packet: $packet")
            packetHandler.handlePacket(packet)
        }
    }

    /**
     * 打开会话，开始连接。
     * 仅在 IDLE 状态下有效，将状态切换为 STARTING 并执行连接。
     */
    override fun open() {
        if (state == State.IDLE) {
            state = State.STARTING
            logger.i("session is opening")
            socketClient.onConnecting(this)
            socketClient.scope.launch {
                if (performConnect()) {
                    onSessionReady()
                }
            }
        }
    }

    /**
     * 执行具体的连接逻辑，由子类实现。
     * 应包含 Socket 连接建立、SSL 握手等操作。
     *
     * @return true 如果连接成功，false 如果连接失败
     */
    protected abstract suspend fun performConnect(): Boolean

    /**
     * 创建读取器，由子类实现。
     *
     * @return 读取器实例
     */
    protected abstract fun createReader(): Reader

    /**
     * 创建写入器，由子类实现。
     *
     * @return 写入器实例
     */
    protected abstract fun createWriter(): Writer

    /**
     * 关闭会话。
     * 如果会话处于 IDLE 或 STARTING 状态，直接标记为 DETACHED 并通知连接失败；
     * 否则通过 [onError] 执行完整的断开流程。
     *
     * @param e 描述关闭原因的异常
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
     * 当底层连接就绪时调用。
     * 启动读取器和写入器，注册响应包处理器，执行会话初始化。
     */
    protected suspend fun onSessionReady() {
        if (state == State.STARTING) {
            // 启动读取器和写入器
            reader = createReader().apply { start() }
            writer = createWriter()

            // 注册响应包处理器
            messageHandlerMap[PacketType.RESPONSE] = taskManager
            state = State.CONNECTED
            logger.i("session start success")
            socketClient.onConnected(this)

            // 执行会话初始化（如登录），无初始化器则直接可用
            val initializer = socketClient.sessionInitializer
            if (initializer == null) {
                onAvailable()
            } else {
                initializer.start(this)
                    .onSuccess { onAvailable() }
                    .onFailure {
                        val e = EasyException(
                            ErrorCode.SESSION_INIT_FAILED, ErrorType.CONNECT,
                            "session initializing failed", suffix, it
                        )
                        onError(e)
                    }
            }
        } else {
            // 状态已变更（可能已被关闭），直接关闭 Socket
            closeSocket()
        }
    }

    /**
     * 连接失败时调用。
     * 将状态切换为 DETACHED 并通知客户端。
     *
     * @param e 描述失败原因的异常
     */
    protected fun onFailed(e: EasyException) {
        if (state == State.STARTING) {
            state = State.DETACHED
            logger.i("session start failed, error: " + e.message)
            socketClient.onConnectFailed(this, e)
        }
    }

    /**
     * 会话变为可用状态。
     * 启动心跳机制，注册推送管理器，通知客户端连接可用。
     */
    private fun onAvailable() {
        if (state == State.CONNECTED) {
            // 启动心跳
            val pulse = Pulse(socketClient, this)
            pulse.start()
            messageHandlerMap[PacketType.PULSE] = pulse
            this.pulse = pulse

            // 注册推送管理器
            socketClient.pushManager?.let { messageHandlerMap[PacketType.PUSH] = it }

            state = State.AVAILABLE
            serverAvailable = true
            logger.i("session is available")
            socketClient.onAvailable(this)
        }
    }

    /**
     * 处理连接错误。
     * 停止心跳、关闭读写器和 Socket，通知客户端连接断开。
     *
     * @param e 描述错误原因的异常
     */
    protected open fun onError(e: EasyException) {
        if (state == State.CONNECTED || state == State.AVAILABLE) {
            // 停止心跳
            pulse?.stop()
            // 关闭读写器
            reader?.shutdown()
            writer?.shutdown()
            // 关闭底层 Socket
            closeSocket()
            state = State.DETACHED
            logger.i("session is closed, error: " + e.message)
            socketClient.onDisconnected(this, e)
        }
    }

    /**
     * 关闭底层的 Socket 资源。
     * 由子类实现具体的 Socket 关闭逻辑。
     */
    protected abstract fun closeSocket()

    /**
     * 获取总连接耗时。
     *
     * @return 连接耗时（毫秒），未记录时返回 -1
     */
    override fun connectTime(): Long = connectTimeMap[Period.ALL] ?: -1

    /**
     * 获取指定阶段的连接耗时。
     *
     * @param period 连接阶段
     * @return 该阶段耗时（毫秒），未记录时返回 -1
     */
    override fun connectTime(period: Period): Long = connectTimeMap[period] ?: -1

    override fun toString(): String = "${this::class.simpleName}[id=$id, address=$address]"

    protected class Stopwatch(private val startTime: Long = Platform.currentTimeMillis()) {
        private var lastTime = startTime

        fun record(period: Period, map: MutableMap<Period, Long>) {
            val now = Platform.currentTimeMillis()
            map[period] = now - lastTime
            lastTime = now
        }

        fun recordTotal(map: MutableMap<Period, Long>): Long {
            val now = Platform.currentTimeMillis()
            val total = now - startTime
            map[Period.ALL] = total
            return total
        }
    }
}