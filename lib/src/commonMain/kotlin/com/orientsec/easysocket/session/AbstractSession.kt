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
import kotlinx.coroutines.launch

/**
 * Session 的抽象基类，封装了状态管理、包分发和生命周期逻辑。
 */
abstract class AbstractSession(
    protected val socketClient: BaseSocketClient,
    override val address: Address,
    override val addressIndex: Int,
    protected val id: Long
) : OperableSession {

    override val suffix: String = ("  session(" + id + ")[" + address.host + ":"
            + address.port + "]  client[" + socketClient.options.name + "]")

    protected val options: Options = socketClient.options
    override val logger: Logger = LogFactory.getLogger(options, suffix)
    protected val taskManager: TaskManager = socketClient.getTaskManager()

    protected var state = State.IDLE
    protected var serverAvailable = false
    protected var pulse: Pulse? = null
    protected var reader: Reader? = null
    override var writer: Writer? = null
        protected set

    protected val messageHandlerMap: MutableMap<PacketType, PacketHandler> = mutableMapOf()
    protected val connectTimeMap: MutableMap<Period, Long> = mutableMapOf()

    override val isConnect: Boolean get() = state == State.CONNECTED || state == State.AVAILABLE
    override val isAvailable: Boolean get() = state == State.AVAILABLE
    override val isServerAvailable: Boolean get() = serverAvailable

    override fun <T> buildTask(request: Request<T>, callback: Callback<T>): Task<T> {
        return TaskImpl(
            TaskType.INITIALIZE, taskManager.generateTaskId(),
            request, callback, socketClient, this
        )
    }

    override fun handlePacket(packet: Packet) {
        socketClient.scope.launch { onPacket(packet) }
    }

    private fun onPacket(packet: Packet) {
        if (state == State.DETACHED) return
        val packetHandler = messageHandlerMap[packet.type]
        if (packetHandler == null) {
            logger.w("no packet handler for " + packet.type)
        } else {
            logger.d("receive a packet: $packet")
            packetHandler.handlePacket(packet)
        }
    }

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
     */
    protected abstract suspend fun performConnect(): Boolean

    protected abstract fun getReader(): Reader

    protected abstract fun getWriter(): Writer

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
     */
    protected suspend fun onSessionReady() {
        if (state == State.STARTING) {
            reader = getReader().apply { start() }
            writer = getWriter()
            messageHandlerMap[PacketType.RESPONSE] = taskManager
            state = State.CONNECTED
            logger.i("session start success")
            socketClient.onConnected(this)

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
            closeSocket()
        }
    }

    protected fun onFailed(e: EasyException) {
        if (state == State.STARTING) {
            state = State.DETACHED
            logger.i("session start failed, error: " + e.message)
            socketClient.onConnectFailed(this, e)
        }
    }

    private fun onAvailable() {
        if (state == State.CONNECTED) {
            val pulse = Pulse(socketClient, this)
            pulse.start()
            messageHandlerMap[PacketType.PULSE] = pulse
            this.pulse = pulse
            socketClient.pushManager?.let { messageHandlerMap[PacketType.PUSH] = it }
            state = State.AVAILABLE
            serverAvailable = true
            logger.i("session is available")
            socketClient.onAvailable(this)
        }
    }

    protected open fun onError(e: EasyException) {
        if (state == State.CONNECTED || state == State.AVAILABLE) {
            pulse?.stop()
            reader?.shutdown()
            writer?.shutdown()
            closeSocket()
            state = State.DETACHED
            logger.i("session is closed, error: " + e.message)
            socketClient.onDisconnected(this, e)
        }
    }

    /**
     * 关闭底层的 Socket 资源。
     */
    protected abstract fun closeSocket()

    override fun connectTime(): Long = connectTimeMap[Period.ALL] ?: -1

    override fun connectTime(period: Period): Long = connectTimeMap[period] ?: -1

    override fun toString(): String = "${this::class.simpleName}[id=$id, address=$address]"
}
