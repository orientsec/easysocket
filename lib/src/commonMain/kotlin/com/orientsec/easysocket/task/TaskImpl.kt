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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds

/**
 * 请求任务的实现类，基于 Kotlin 协程实现。
 *
 * 管理一个请求从创建到完成的完整生命周期：
 * 1. **启动** - 注册到任务管理器，如果连接不可用则进入等待队列
 * 2. **编码** - 将请求数据编码为字节数组
 * 3. **发送** - 通过 Writer 将数据写入 Socket
 * 4. **等待响应** - 发送成功后启动超时监控
 * 5. **解码** - 接收到响应包后解码为结果
 * 6. **完成** - 通知回调成功或失败
 *
 * 支持任务重试：当连接不可用导致任务失败时，可以自动重试，
 * 重试次数由 [Options.taskRetryTimes] 控制。
 *
 * @param T 响应数据类型
 */
@OptIn(ExperimentalAtomicApi::class)
class TaskImpl<T> : OperableTask<T> {
    /** 是否已执行过，防止重复执行 */
    private val executed = AtomicBoolean(false)

    /** 所属的 Socket 客户端 */
    private val socketClient: BaseSocketClient

    private val scope: CoroutineScope

    /** 请求对象 */
    private val request: Request<T>

    /** 生命周期回调包装器，负责线程调度和日志 */
    private val callback: LifecycleCallback<T>

    /** 配置选项 */
    private val options: Options

    /** 任务唯一标识 */
    override val taskId: Int

    /** 任务类型 */
    override val taskType: TaskType

    /** 任务管理器 */
    private val taskManager: TaskManager

    /** 关联的会话，初始化任务和心跳任务需要指定 */
    private val session: OperableSession?

    /** 编码后的请求数据 */
    override var data: ByteArray? = null
        private set

    /** 解码后的响应结果 */
    @Volatile
    override var response: T? = null
        private set

    /** 任务执行过程中的错误 */
    @Volatile
    override var error: Throwable? = null
        private set

    /** 用于 await() 模式的 CompletableDeferred */
    private val resultDeferred = CompletableDeferred<T>()

    /** 用于等待响应包的 CompletableDeferred */
    private var packetDeferred: CompletableDeferred<Packet>? = null

    /** 是否已成功发送数据（发送后不再允许重试） */
    private var hasSentData = false

    /** 当前重试次数 */
    private var retryTimes = 0

    /**
     * 创建普通请求任务的构造函数。
     *
     * @param taskId 任务ID
     * @param request 请求对象
     * @param callback 回调接口
     * @param socketClient Socket 客户端
     */
    constructor(
        taskId: Int,
        request: Request<T>,
        callback: Callback<T>,
        socketClient: BaseSocketClient
    ) : this(TaskType.REQUEST, taskId, request, callback, socketClient, null)

    /**
     * 完整构造函数，支持指定任务类型和关联会话。
     *
     * @param taskType 任务类型
     * @param taskId 任务ID
     * @param request 请求对象
     * @param callback 回调接口
     * @param socketClient Socket 客户端
     * @param session 关联的会话，初始化任务和心跳任务需要指定
     */
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
        this.scope = socketClient.scope
    }

    /** 任务是否已完成 */
    override val isCompleted: Boolean get() = resultDeferred.isCompleted

    /** 任务是否成功完成 */
    override val isSuccess: Boolean
        get() = resultDeferred.isCompleted
                && !resultDeferred.isCancelled && error == null

    /** 任务是否失败 */
    override val isFailure: Boolean get() = error != null

    /** 任务是否已取消 */
    override val isCanceled: Boolean get() = resultDeferred.isCancelled

    /**
     * 获取关联的请求对象。
     *
     * @return 请求对象
     */
    override fun request(): Request<T> = request

    /**
     * 执行任务。
     * 将任务分发到串行调度器执行，确保线程安全。
     */
    override fun execute() {
        if (!executed.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch {
            if (socketClient.isShutdown()) {
                EasyException(
                    ErrorCode.SHUTDOWN,
                    ErrorType.SYSTEM,
                    "socket client is shutdown",
                    socketClient.suffix
                ).let { onError(it) }
                return@launch
            }
            callback.onStart()
            taskManager.addTask(this@TaskImpl)

            if (taskType == TaskType.REQUEST) {
                socketClient.start()
                if (!socketClient.isAvailable()) {
                    taskManager.addTaskToWaitingQueue(this@TaskImpl)
                    callback.onWait()
                    return@launch
                }
            }
            startEncoding()
        }
    }

    /**
     * 挂起等待任务完成并返回结果。
     * 如果任务尚未执行，会自动执行。
     *
     * @return 响应数据
     * @throws Throwable 任务执行过程中的异常
     */
    override suspend fun await(): T {
        execute()
        return resultDeferred.await()
    }

    /**
     * 任务恢复执行。
     * 当连接变为可用时，由任务管理器在串行调度器中调用。
     */
    override fun onResume() {
        callback.onResume()
        taskManager.addTask(this@TaskImpl)
        socketClient.scope.launch {
            startEncoding()
        }
    }

    /**
     * 启动编码流程。
     * 在协程中执行请求编码，然后将编码后的数据提交给 Writer。
     */
    private suspend fun startEncoding() {
        // 编码请求数据
        callback.onEncodeStart()
        val encodedData = try {
            withContext(options.codecDispatcher) {
                request.encode(taskId)
            }
        } catch (t: Throwable) {
            onError(t)
            return
        }
        if (encodedData.isEmpty()) {
            EasyException(
                ErrorCode.REQUEST_DATA_EMPTY,
                ErrorType.TASK,
                "request data is empty",
                socketClient.suffix
            ).let { onError(it) }
            return
        }
        data = encodedData
        callback.onEncodeSuccess()

        // 提交给 Writer 发送
        val writer = getWriter()
        if (writer == null) {
            // session失效，没有可用的 Writer，记录日志并返回
            socketClient.logger.i("no available writer, taskId: $taskId")
            return
        }
        writer.submit(this@TaskImpl)
    }

    /**
     * 数据开始发送时的回调。
     * 在串行调度器中标记已发送数据。
     */
    override fun onSendStart() {
        hasSentData = true
        callback.onSendStart()
    }

    /**
     * 数据发送成功后的处理。
     * 启动超时监控，等待服务器响应。
     */
    override fun onSendSuccess() {
        callback.onSendSuccess()
        // 数据写入成功后，启动超时监控等待响应
        socketClient.scope.launch {
            try {
                withTimeout(options.requestTimeoutMills.milliseconds) {
                    val packetDeferred = CompletableDeferred<Packet>()
                    this@TaskImpl.packetDeferred = packetDeferred
                    val packet = packetDeferred.await()
                    handleResponse(packet)
                }
            } catch (_: TimeoutCancellationException) {
                EasyException(
                    ErrorCode.RESPONSE_TIME_OUT, ErrorType.TASK,
                    "response time out", socketClient.suffix
                ).let { onError(it) }
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    /**
     * 数据发送失败时的回调。
     */
    override fun onSendFailure(t: Throwable) {
        callback.onSendFailure(t)
    }

    /**
     * 接收到响应包时的回调。
     * 完成 packetDeferred，触发响应处理流程。
     *
     * @param packet 接收到的数据包
     */
    override fun onPacketReceived(packet: Packet) {
        packetDeferred?.complete(packet)
    }

    /**
     * 处理服务器响应。
     * 解码响应数据并通知成功。
     *
     * @param packet 服务器返回的数据包
     */
    private suspend fun handleResponse(packet: Packet) {
        callback.onPacketReceived(packet)

        // 解码响应
        callback.onDecodeStart()
        val result = withContext(options.codecDispatcher) {
            request.decode(packet)
        }
        callback.onDecodeSuccess()

        onSuccess(result)
    }

    /**
     * 任务成功完成。
     * 设置响应结果，完成 Deferred，通知回调。
     *
     * @param res 响应结果
     */
    private fun onSuccess(res: T) {
        if (!isCompleted) {
            response = res
            resultDeferred.complete(res)
            callback.onSuccess(res)
        }
    }

    /**
     * 任务失败。
     * 设置错误信息，从管理器移除，完成 Deferred，通知回调。
     *
     * @param t 失败原因
     */
    private fun onError(t: Throwable) {
        if (!isCompleted) {
            this.error = t
            taskManager.removeTask(this)
            packetDeferred?.cancel()
            resultDeferred.completeExceptionally(t)
            callback.onFailure(t)
        }
    }

    /**
     * 取消任务。
     * 异步分发到串行调度器执行。
     */
    override fun cancel() {
        if (isCompleted) return
        socketClient.scope.launch {
            if (!isCompleted) {
                taskManager.cancelTask(this@TaskImpl)
                getWriter()?.cancel(this@TaskImpl)
                packetDeferred?.cancel()
                resultDeferred.cancel()
                callback.onCanceled()
            }
        }
    }

    /**
     * 获取当前可用的 Writer。
     * 优先使用关联会话的 Writer，其次使用客户端当前会话的 Writer。
     *
     * @return 可用的 Writer，如果没有可用会话则返回 null
     */
    private fun getWriter(): Writer? {
        if (session != null) return session.writer
        val session = socketClient.session
        return if (session != null && session.isAvailable) session.writer else null
    }

    /**
     * 尝试重置任务以进行重试。
     * 仅在以下条件满足时允许重试：
     * - 任务尚未完成
     * - 任务类型为 REQUEST
     * - 尚未发送数据
     * - 重试次数未超过限制
     * - 错误类型不是 SYSTEM
     *
     * @param t 导致重试的错误
     * @return true 如果任务已重置并等待重试，false 如果不可重试
     */
    override fun onReset(t: Throwable): Boolean {
        if (isCompleted) return false
        if (taskType != TaskType.REQUEST || hasSentData
            || retryTimes >= options.taskRetryTimes
            || (t is EasyException && t.type == ErrorType.SYSTEM)
        ) {
            onError(t)
            return false
        }
        retryTimes++
        data = null
        callback.onReset(retryTimes, t)
        callback.onWait()
        return true
    }
}