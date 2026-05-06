package com.orientsec.easysocket.client

import android.R.attr.name
import com.orientsec.easysocket.*
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.Session
import com.orientsec.easysocket.socket.SocketSession
import com.orientsec.easysocket.task.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor

/**
 * EasySocketClient is a concrete implementation of [BaseSocketClient] that manages
 * socket connections, handles initialization, reconnection, and task execution.
 */
class EasySocketClient(options: Options, scope: CoroutineScope) :
    BaseSocketClient(options, scope) {

    private val name: String = options.name
    private val reconnectManager: ReconnectManager = ReconnectManager(this)
    private val taskManager: TaskManager = TaskManagerImpl(logger)
    private val callbackExecutor: Executor = options.callbackExecutor
    private val connectionListeners = CopyOnWriteArraySet<ConnectionListener>()
    private var state = STATE_ACTIVE
    private var activeTimestamp: Long = 0
    private var socketSession: SocketSession? = null

    private var addressList: List<Address> = emptyList()

    private var isInitializing = false
    private var failedTimes = 0
    private var addressIndex = 0
    private var sessionId: Long = 0

    override fun <T> buildTask(request: Request<T>, callback: Callback<T>): Task<T> {
        return TaskImpl(taskManager.generateTaskId(), request, callback, this)
    }

    override fun addConnectionListener(listener: ConnectionListener) {
        connectionListeners.add(listener)
    }

    override fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners.remove(listener)
    }

    override fun getTaskManager(): TaskManager {
        return taskManager
    }

    override val session: OperableSession?
        get() = socketSession

    override fun setAddressList(addressList: List<Address>) {
        require(addressList.isNotEmpty()) { "address list must not be empty" }
        scope.launch {
            this@EasySocketClient.addressList = addressList
            addressIndex = 0
        }
    }

    fun onStart(active: Boolean = true) {
        if (isShutdown()) return
        if (active) {
            state = STATE_ACTIVE
            activeTimestamp = System.currentTimeMillis()
        }
        if (socketSession == null && prepareAddressList()) {
            val address = addressList[addressIndex]
            socketSession = SocketSession(this, address, addressIndex, sessionId++)
                .apply { open() }
        }
    }

    /**
     * Prepares the address list for the socket client.
     *
     * @return `true` if the address list is ready, `false` otherwise.
     */
    private fun prepareAddressList(): Boolean {
        if (addressList.isNotEmpty()) return true
        if (isInitializing) {
            logger.d("client is initializing, just wait for the result")
            return false
        }

        val optionsAddressList = options.addressList
        if (!optionsAddressList.isNullOrEmpty()) {
            addressList = optionsAddressList
            addressIndex = 0
            return true
        }

        val initializer = clientInitializer
        if (initializer == null) {
            logger.e("ClientInitializer is null")
            handleAddressListError("ClientInitializer is null")
            return false
        }

        isInitializing = true
        try {
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
            }
        } finally {
            isInitializing = false
        }
        return false
    }

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

    override fun onConnecting(session: Session) {
        assert(session === this.socketSession)
        if (connectionListeners.isNotEmpty()) {
            callbackExecutor.execute {
                for (listener in connectionListeners) {
                    listener.onConnecting(session)
                }
            }
        }
    }

    override fun onConnected(session: Session) {
        assert(session === this.socketSession)
        if (connectionListeners.isNotEmpty()) {
            callbackExecutor.execute {
                for (listener in connectionListeners) {
                    listener.onConnected(session)
                }
            }
        }
    }

    override fun onConnectFailed(session: Session, e: EasyException) {
        assert(session === this.socketSession)
        this.socketSession = null
        taskManager.reset(e)
        reconnectManager.delayedReconnect(session)

        if (connectionListeners.isNotEmpty()) {
            callbackExecutor.execute {
                for (listener in connectionListeners) {
                    listener.onConnectFailed(session, e)
                }
            }
        }
    }

    override fun onDisconnected(session: Session, e: EasyException) {
        assert(session === this.socketSession)
        this.socketSession = null
        taskManager.reset(e)
        reconnectManager.delayedReconnect(session)

        if (connectionListeners.isNotEmpty()) {
            callbackExecutor.execute {
                for (listener in connectionListeners) {
                    listener.onDisconnected(session, e)
                }
            }
        }
    }

    override fun onAvailable(session: Session) {
        assert(session === this.socketSession)
        failedTimes = 0
        taskManager.ready()

        if (connectionListeners.isNotEmpty()) {
            callbackExecutor.execute {
                for (listener in connectionListeners) {
                    listener.onAvailable(session)
                }
            }
        }
    }

    override fun start() {
        if (isShutdown()) return
        scope.launch { onStart() }
    }

    override fun stop() {
        if (isShutdown()) return
        scope.launch {
            if (isShutdown()) return@launch
            logger.i("stop socket client")
            state = STATE_SLEEP
            socketSession?.let {
                val e = EasyException(
                    ErrorCode.STOP, ErrorType.SYSTEM,
                    "socket client is stopped", it.suffix
                )
                it.close(e)
            }
        }
    }

    override fun shutdown() {
        if (isShutdown()) return
        scope.launch {
            if (isShutdown()) return@launch
            logger.i("shutdown socket client")
            state = STATE_SHUTDOWN
            val e = EasyException(
                ErrorCode.SHUTDOWN, ErrorType.SYSTEM,
                "socket client on shutdown", suffix
            )
            socketSession?.close(e) ?: taskManager.reset(e)
            EasySocket.removeSocketClient(this@EasySocketClient)
        }
    }

    override fun isShutdown(): Boolean {
        return state == STATE_SHUTDOWN
    }

    override fun isConnected(): Boolean {
        return socketSession?.isConnect ?: false
    }

    override fun isAvailable(): Boolean {
        return socketSession?.isAvailable ?: false
    }

    override fun onNetworkAvailable() {
        if (state == STATE_ACTIVE && socketSession == null) {
            reconnectManager.reconnect()
        }
    }

    fun switchServer() {
        if (++failedTimes >= options.retryTimesPerAddress) {
            failedTimes = 0
            addressIndex = (addressIndex + 1) % addressList.size
            logger.i("switch to server: ${addressList[addressIndex]}")
        }
    }

    fun isActive(): Boolean {
        if (state != STATE_ACTIVE) return false

        val backgroundTimestamp = EasySocket.getBackgroundTimestamp()
        if (backgroundTimestamp == 0L) return true

        val currentTimeMillis = System.currentTimeMillis()
        val backgroundActiveDurationSeconds = options.backgroundActiveDurationSeconds * 1000L
        return currentTimeMillis - backgroundTimestamp <= backgroundActiveDurationSeconds &&
                currentTimeMillis - activeTimestamp <= backgroundActiveDurationSeconds
    }

    override fun toString(): String {
        return "EasySocketClient[name=$name]"
    }

    companion object {
        private const val STATE_ACTIVE = 0
        private const val STATE_SLEEP = 1
        private const val STATE_SHUTDOWN = 2
    }
}
