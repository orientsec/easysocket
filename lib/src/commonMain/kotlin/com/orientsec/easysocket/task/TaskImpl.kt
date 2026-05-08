package com.orientsec.easysocket.task

import com.orientsec.easysocket.Options
import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.Writer
import kotlinx.coroutines.*

/**
 * Represents a request task implemented using Kotlin Coroutines.
 *
 * @param T The type of the response data.
 */
class TaskImpl<T> : OperableTask<T> {
    private var executed = false
    private val socketClient: BaseSocketClient
    private val request: Request<T>
    private val callback: LifecycleCallback<T>
    private val options: Options
    override val taskId: Int
    override val taskType: TaskType

    private val taskManager: TaskManager
    private val session: OperableSession?

    @Volatile
    override var data: ByteArray? = null
        private set

    @Volatile
    override var response: T? = null
        private set

    @Volatile
    override var error: Throwable? = null
        private set

    private val resultDeferred = CompletableDeferred<T>()
    private var packetDeferred = CompletableDeferred<Packet>()

    private var hasSentData = false
    private var retryTimes = 0

    constructor(
        taskId: Int,
        request: Request<T>,
        callback: Callback<T>,
        socketClient: BaseSocketClient
    ) : this(TaskType.REQUEST, taskId, request, callback, socketClient, null)

    constructor(
        taskType: TaskType,
        taskId: Int,
        request: Request<T>,
        callback: Callback<T>,
        socketClient: BaseSocketClient,
        session: OperableSession?
    ) {
        this.taskType = taskType
        this.taskId = taskId
        this.request = request
        this.options = socketClient.options
        this.callback = LifecycleCallbackWrapper(callback, this, socketClient)
        this.socketClient = socketClient
        this.taskManager = socketClient.getTaskManager()
        this.session = session
    }

    override val isCompleted: Boolean get() = resultDeferred.isCompleted
    override val isSuccess: Boolean get() = resultDeferred.isCompleted && !resultDeferred.isCancelled && error == null
    override val isFailure: Boolean get() = error != null
    override val isCanceled: Boolean get() = resultDeferred.isCancelled

    override fun request(): Request<T> = request

    @Synchronized
    override fun execute() {
        if (!executed) {
            executed = true
            doExecute()
        } else {
            throw IllegalStateException("Task is already executed")
        }
    }

    override suspend fun await(): T {
        if (!executed) {
            execute()
        }
        return resultDeferred.await()
    }

    override fun onResume() {
        callback.onResume()
        doExecute()
    }

    /**
     * doExecute 不再封装大协程，通过同步方法 onStart 驱动状态流转。
     */
    private fun doExecute() {
        try {
            packetDeferred = CompletableDeferred()
            val isWait = onStart()
            if (!isWait) {
                startEncoding()
            }
        } catch (t: Throwable) {
            if (!onReset(t)) {
                onError(t)
            }
        }
    }

    private fun startEncoding() {
        socketClient.scope.launch {
            try {
                // Encode
                callback.onEncodeStart()
                val encodedData = withContext(options.codecDispatcher) {
                    request.encode(taskId)
                }
                if (encodedData.isEmpty()) {
                    throw EasyException(
                        ErrorCode.REQUEST_DATA_EMPTY,
                        ErrorType.TASK,
                        "request data is empty",
                        socketClient.suffix
                    )
                }
                data = encodedData
                callback.onEncodeSuccess()

                // Submit
                val writer = getWriter() ?: throw EasyException(
                    ErrorCode.SOCKET_CONNECT,
                    ErrorType.CONNECT,
                    "no available session",
                    socketClient.suffix
                )
                writer.submit(this@TaskImpl)
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    onCancel()
                } else if (!onReset(t)) {
                    onError(t)
                }
            }
        }
    }

    /**
     * @return true if task is waiting for connection, false if it can proceed.
     */
    private fun onStart(): Boolean {
        if (socketClient.isShutdown()) {
            throw EasyException(
                ErrorCode.SHUTDOWN,
                ErrorType.SYSTEM,
                "socket client is shutdown",
                socketClient.suffix
            )
        }
        callback.onStart()
        taskManager.addTask(this)

        if (taskType == TaskType.REQUEST) {
            socketClient.start()
            if (!socketClient.isAvailable()) {
                taskManager.addTaskToWaitingQueue(this)
                callback.onWait()
                return true
            }
        }
        return false
    }

    override fun onSendStart() {
        hasSentData = true
        callback.onSendStart()
    }

    override fun onSendSuccess() {
        callback.onSendSuccess()
        // 数据写入 OutputStream 成功后，启动超时监控
        socketClient.scope.launch {
            try {
                withTimeout(options.requestTimeoutMillis.toLong()) {
                    val packet = packetDeferred.await()
                    handleResponse(packet)
                }
            } catch (t: Throwable) {
                if (t !is CancellationException) {
                    if (!onReset(t)) {
                        onError(t)
                    }
                }
            }
        }
    }

    override fun onSendFailure(t: Throwable) {
        callback.onSendFailure(t)
        packetDeferred.completeExceptionally(t)
    }

    override fun onPacketReceived(packet: Packet) {
        packetDeferred.complete(packet)
    }

    private suspend fun handleResponse(packet: Packet) {
        callback.onPacketReceived(packet)

        // Decode
        callback.onDecodeStart()
        val result = withContext(options.codecDispatcher) {
            request.decode(packet)
        }
        callback.onDecodeSuccess()

        onSuccess(result)
    }

    private fun onSuccess(res: T) {
        if (!isCompleted) {
            response = res
            resultDeferred.complete(res)
            callback.onSuccess(res)
        }
    }

    private fun onError(t: Throwable) {
        if (!isCompleted) {
            this.error = t
            taskManager.removeTask(this)
            resultDeferred.completeExceptionally(t)
            callback.onFailure(t)
        }
    }

    override fun cancel() {
        onCancel()
    }

    private fun onCancel() {
        if (!isCompleted) {
            taskManager.cancelTask(this)
            getWriter()?.cancel(this)
            packetDeferred.cancel()
            resultDeferred.cancel()
            callback.onCanceled()
        }
    }

    private fun getWriter(): Writer? {
        if (session != null) return session.writer
        val session = socketClient.session
        return if (session != null && session.isAvailable) session.writer else null
    }

    override fun onReset(t: Throwable): Boolean {
        if (isCompleted) return false
        if (taskType != TaskType.REQUEST || hasSentData
            || retryTimes >= options.taskRetryTimes
            || (t is EasyException && t.type == ErrorType.SYSTEM)
        ) {
            return false
        }
        retryTimes++
        data = null
        callback.onReset(retryTimes, t)
        callback.onWait()
        return true
    }
}
