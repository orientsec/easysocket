package com.orientsec.easysocket.client

import com.orientsec.easysocket.*
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.Session
import com.orientsec.easysocket.task.*
import com.orientsec.easysocket.utils.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * EasySocket 客户端的具体实现类。
 *
 * 管理完整的 Socket 连接生命周期，包括：
 * - 连接的启动、停止和关闭
 * - 服务器地址列表的初始化和切换
 * - 连接失败后的自动重连
 * - 任务管理（请求的发送与响应处理）
 * - 连接事件监听器的管理
 *
 * 状态流转：
 * ```
 * ACTIVE -> SLEEP (调用 stop)
 * ACTIVE -> SHUTDOWN (调用 shutdown)
 * SLEEP -> ACTIVE (调用 start)
 * ```
 */
@OptIn(ExperimentalAtomicApi::class)
class EasySocketClient(options: Options, scope: CoroutineScope) :
    BaseSocketClient(options, scope) {

    /** 客户端名称，用于日志标识 */
    private val name: String = options.name

    /** 重连管理器，负责连接失败后的延迟重连逻辑 */
    private val reconnectManager: ReconnectManager = ReconnectManager(this)

    /** 任务管理器，负责请求任务的创建、调度和生命周期管理 */
    private val taskManager: TaskManager = TaskManagerImpl(logger)

    /** 连接事件监听器列表 */
    private val connectionListeners = mutableListOf<ConnectionListener>()

    /** 监听器列表的互斥锁，确保线程安全 */
    private val listenersMutex = Mutex()

    /** 当前客户端状态：STATE_ACTIVE、STATE_SLEEP 或 STATE_SHUTDOWN */
    private val state = AtomicInt(STATE_ACTIVE)

    /** 客户端变为活跃状态的时间戳 */
    @Volatile
    private var activeTimestamp: Long = 0

    /** 当前活跃的 Socket 会话 */
    @Volatile
    private var socketSession: OperableSession? = null

    /** 服务器地址列表 */
    private var addressList: List<Address> = emptyList()

    /** 是否正在通过 ClientInitializer 初始化地址列表 */
    private var isInitializing = false

    /** 当前地址的连续失败次数 */
    private var failedTimes = 0

    /** 当前使用的地址索引 */
    private var addressIndex = 0

    /** 会话 ID 计数器，用于唯一标识每个会话 */
    private var sessionId: Long = 0

    /**
     * 构建一个请求任务。
     *
     * @param T 响应数据类型
     * @param request 请求对象
     * @param callback 回调接口
     * @return 可执行的 [Task] 实例
     */
    override fun <T> buildTask(request: Request<T>, callback: Callback<T>): Task<T> {
        return TaskImpl(taskManager.generateTaskId(), request, callback, this)
    }

    /**
     * 添加连接事件监听器。
     * 监听器回调会在 [com.orientsec.easysocket.Options.callbackDispatcher] 指定的调度器上执行。
     *
     * @param listener 要添加的监听器
     */
    override fun addConnectionListener(listener: ConnectionListener) {
        scope.launch {
            listenersMutex.withLock {
                connectionListeners.add(listener)
            }
        }
    }

    /**
     * 移除连接事件监听器。
     *
     * @param listener 要移除的监听器
     */
    override fun removeConnectionListener(listener: ConnectionListener) {
        scope.launch {
            listenersMutex.withLock {
                connectionListeners.remove(listener)
            }
        }
    }

    override fun getTaskManager(): TaskManager {
        return taskManager
    }

    /** 当前活跃的会话实例 */
    override val session: OperableSession?
        get() = socketSession

    /**
     * 设置服务器地址列表。
     * 设置后会重置地址索引为 0。
     *
     * @param addressList 新的地址列表，不能为空
     * @throws IllegalArgumentException 如果地址列表为空
     */
    override fun setAddressList(addressList: List<Address>) {
        require(addressList.isNotEmpty()) { "address list must not be empty" }
        scope.launch {
            this@EasySocketClient.addressList = addressList
            addressIndex = 0
        }
    }

    /**
     * 启动连接。
     * 如果客户端处于活跃状态，会尝试创建新会话并连接。
     *
     */
    fun onStart() {
        if (state.load() != STATE_ACTIVE) return

        // 准备地址列表后创建会话
        if (socketSession == null && prepareAddressList()) {
            val address = addressList[addressIndex]
            socketSession = options.sessionFactory.createSession(
                this,
                address,
                addressIndex,
                sessionId++
            ).apply { open() }
        }
    }

    /**
     * 准备服务器地址列表。
     * 优先使用 Options 中配置的静态地址，其次通过 ClientInitializer 动态获取。
     *
     * @return true 如果地址列表已就绪，false 如果正在异步获取
     */
    private fun prepareAddressList(): Boolean {
        // 地址列表已就绪
        if (addressList.isNotEmpty()) return true
        // 正在初始化中，等待结果
        if (isInitializing) {
            logger.d("client is initializing, just wait for the result")
            return false
        }

        // 尝试使用 Options 中配置的静态地址
        val optionsAddressList = options.addressList
        if (!optionsAddressList.isNullOrEmpty()) {
            addressList = optionsAddressList
            addressIndex = 0
            return true
        }

        // 通过 ClientInitializer 动态获取地址
        val initializer = clientInitializer
        if (initializer == null) {
            logger.e("ClientInitializer is null")
            handleAddressListError("ClientInitializer is null")
            return false
        }

        isInitializing = true
        scope.launch {
            initializer.getAddressList().onSuccess {
                if (it.isEmpty()) {
                    logger.e("address list is empty")
                    handleAddressListError("address list is empty")
                } else {
                    addressList = it
                    addressIndex = 0
                    onStart()
                }
            }.onFailure {
                logger.e("socket client initialize failed, error: " + it.message)
                handleAddressListError("socket client initialize failed", it)
            }
            isInitializing = false
        }
        return false
    }

    /**
     * 处理地址列表获取失败的错误。
     * 将错误传递给任务管理器，使所有等待中的任务失败。
     *
     * @param msg 错误信息
     * @param t 异常原因
     */
    private fun handleAddressListError(msg: String, t: Throwable? = null) {
        val e = EasyException(
            code = ErrorCode.INIT_FAILED,
            type = ErrorType.SYSTEM,
            message = msg,
            suffix = suffix,
            cause = t
        )
        taskManager.reset(e)
    }

    /**
     * 通知所有注册的监听器。
     * 回调在 [com.orientsec.easysocket.Options.callbackDispatcher] 上执行。
     *
     * @param block 对每个监听器执行的操作
     */
    private fun notifyListeners(block: (ConnectionListener) -> Unit) {
        scope.launch(options.callbackDispatcher) {
            val listeners = listenersMutex.withLock { connectionListeners.toList() }
            for (listener in listeners) {
                block(listener)
            }
        }
    }

    /** 连接开始时通知监听器 */
    override fun onConnecting(session: Session) {
        notifyListeners { it.onConnecting(session) }
    }

    /**
     * 连接成功时处理。
     * 不做额外处理，等待会话初始化完成后触发 onAvailable。
     */
    override fun onConnected(session: Session) {
        notifyListeners { it.onConnected(session) }
    }

    /**
     * 连接失败时处理。
     * 清除会话、重置任务、触发延迟重连，并通知监听器。
     */
    override fun onConnectFailed(session: Session, e: EasyException) {
        this.socketSession = null
        taskManager.reset(e)
        reconnectManager.delayedReconnect(session)
        notifyListeners { it.onConnectFailed(session, e) }
    }

    /**
     * 连接断开时处理。
     * 清除会话、重置任务、触发延迟重连，并通知监听器。
     */
    override fun onDisconnected(session: Session, e: EasyException) {
        this.socketSession = null
        taskManager.reset(e)
        reconnectManager.delayedReconnect(session)
        notifyListeners { it.onDisconnected(session, e) }
    }

    /**
     * 连接可用时处理。
     * 重置失败计数，标记任务管理器为就绪状态，通知监听器。
     */
    override fun onAvailable(session: Session) {
        failedTimes = 0
        taskManager.ready()
        notifyListeners { it.onAvailable(session) }
    }

    /**
     * 启动连接。
     * 如果客户端已关闭则不执行任何操作。
     */
    override fun start() {
        if (isShutdown()) return
        activeTimestamp = Platform.currentTimeMillis()

        while (true) {
            val current = state.load()
            if (current == STATE_SHUTDOWN) return

            if (current == STATE_ACTIVE) {
                if (socketSession != null) return
                break // 继续启动协程
            } else if (current == STATE_SLEEP) {
                if (state.compareAndSet(STATE_SLEEP, STATE_ACTIVE)) {
                    break // 成功切换，启动协程
                }
                // CAS 失败，循环重试
            }
        }
        scope.launch { onStart() }
    }

    /**
     * 停止连接。
     * 客户端进入休眠状态，当前会话会被关闭。
     * 可以通过 [start] 重新激活。
     */
    override fun stop() {
        if (state.compareAndSet(STATE_ACTIVE, STATE_SLEEP)) {
            scope.launch {
                if (state.load() != STATE_SLEEP) return@launch
                logger.i("stop socket client")
                socketSession?.let {
                    val e = EasyException(
                        ErrorCode.STOP, ErrorType.SYSTEM,
                        "socket client is stopped", it.suffix
                    )
                    it.close(e)
                }
            }
        }
    }

    /**
     * 关闭客户端。
     * 客户端进入关闭状态，不可再被激活。
     * 当前会话会被关闭，客户端从全局列表中移除。
     */
    override fun shutdown() {
        while (true) {
            val current = state.load()
            if (current == STATE_SHUTDOWN) return
            if (state.compareAndSet(current, STATE_SHUTDOWN)) {
                scope.launch {
                    logger.i("shutdown socket client")
                    val e = EasyException(
                        ErrorCode.SHUTDOWN, ErrorType.SYSTEM,
                        "socket client on shutdown", suffix
                    )
                    socketSession?.close(e) ?: taskManager.reset(e)
                    EasySocket.removeSocketClient(this@EasySocketClient)
                }
                break
            }
        }
    }

    /**
     * 检查客户端是否已关闭。
     * 关闭后的客户端不可再使用。
     *
     * @return true 如果已关闭
     */
    override fun isShutdown(): Boolean {
        return state.load() == STATE_SHUTDOWN
    }

    /**
     * 检查客户端是否已连接。
     *
     * @return true 如果存在活跃的连接
     */
    override fun isConnected(): Boolean {
        return socketSession?.isConnect ?: false
    }

    /**
     * 检查连接是否可用（已连接且已初始化完成）。
     *
     * @return true 如果连接可用
     */
    override fun isAvailable(): Boolean {
        return socketSession?.isAvailable ?: false
    }

    /**
     * 网络恢复时的处理。
     * 如果客户端处于活跃状态且没有活跃会话，则立即尝试重连。
     */
    override fun onNetworkAvailable() {
        if (state.load() == STATE_ACTIVE && socketSession == null) {
            reconnectManager.reconnect()
        }
    }

    /**
     * 切换到下一个服务器地址。
     * 当当前地址连续失败次数达到 [Options.retryTimesPerAddress] 时切换。
     */
    fun switchServer() {
        if (++failedTimes >= options.retryTimesPerAddress) {
            failedTimes = 0
            addressIndex = (addressIndex + 1) % addressList.size
            logger.i("switch to server: ${addressList[addressIndex]}")
        }
    }

    /**
     * 检查客户端是否处于活跃状态。
     * 活跃状态需要同时满足：
     * 1. 客户端状态为 ACTIVE
     * 2. 应用在前台，或进入后台时间未超过 [Options.backgroundActiveDurationSeconds]
     * 3. 客户端活跃时间未超过 [Options.backgroundActiveDurationSeconds]
     *
     * @return true 如果客户端活跃
     */
    fun isActive(): Boolean {
        if (state.load() != STATE_ACTIVE) return false

        val backgroundTimestamp = EasySocket.getBackgroundTimestamp()
        // 前台状态，始终活跃
        if (backgroundTimestamp == 0L) return true

        val currentTimeMillis = Platform.currentTimeMillis()
        val backgroundActiveDurationSeconds = options.backgroundActiveDurationSeconds * 1000L
        // 检查后台持续时间和客户端活跃时间是否在允许范围内
        return currentTimeMillis - backgroundTimestamp <= backgroundActiveDurationSeconds &&
                currentTimeMillis - activeTimestamp <= backgroundActiveDurationSeconds
    }

    override fun toString(): String {
        return "EasySocketClient[name=$name]"
    }

    companion object {
        /** 活跃状态：客户端正常运行 */
        private const val STATE_ACTIVE = 0

        /** 休眠状态：客户端已停止，但可重新启动 */
        private const val STATE_SLEEP = 1

        /** 关闭状态：客户端已关闭，不可再使用 */
        private const val STATE_SHUTDOWN = 2
    }
}