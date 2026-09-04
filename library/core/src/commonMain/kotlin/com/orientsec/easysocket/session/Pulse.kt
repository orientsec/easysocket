package com.orientsec.easysocket.session

import com.orientsec.easysocket.Options
import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.PacketHandler
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.task.Callback
import com.orientsec.easysocket.task.DefaultCallback
import com.orientsec.easysocket.task.Task
import com.orientsec.easysocket.task.TaskBuilder
import com.orientsec.easysocket.task.TaskImpl
import com.orientsec.easysocket.task.TaskType
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * 心跳看门狗，负责维护 Socket 连接的活跃状态。
 *
 * 采用看门狗模式：每次收到数据时调用 [feed] 重置计时器；
 * 若超过 [Options.pulseDelaySeconds] 秒没有数据，则开始发送心跳探测。
 * 探测失败超过 [Options.pulseRetryTimes] 次后，认为连接已断开，关闭会话。
 *
 * 同时实现了 [PacketHandler] 接口，用于处理服务器返回的心跳响应包；
 * 实现了 [TaskBuilder] 接口，用于构建心跳任务。
 *
 * @param socketClient 所属的 Socket 客户端
 * @param session 所属的会话实例
 */
class Pulse(
    private val socketClient: BaseSocketClient,
    private val session: OperableSession
) : PacketHandler, TaskBuilder {

    /** 心跳触发延迟（毫秒），无数据多久后开始探测 */
    private val delayMillis: Long

    /** 心跳重试间隔（毫秒），探测失败后的重试间隔 */
    private val retryIntervalMillis: Long

    /** 心跳最大重试次数，超过后认为连接断开 */
    private val retryTimes: Int

    /** 连续心跳失败次数 */
    private var barkTimes = 0

    /** 协程作用域 */
    val scope: CoroutineScope = socketClient.scope

    /** 配置选项 */
    val options: Options = socketClient.options

    /** 日志记录器 */
    private val logger: Logger

    /** 看门狗协程任务 */
    private var watchDogJob: Job? = null

    init {
        this.delayMillis = options.pulseDelaySeconds * 1000L
        this.retryIntervalMillis = options.pulseRetryIntervalMillis.toLong()
        this.retryTimes = options.pulseRetryTimes
        logger = session.logger
    }

    /**
     * 喂狗：重置心跳失败计数和看门狗计时器。
     * 每次收到数据时调用，表示连接仍然活跃。
     */
    fun feed() {
        barkTimes = 0
        schedule(delayMillis)
    }

    /**
     * 启动心跳机制。
     * 在连接成功并完成初始化后调用。
     */
    fun start() {
        feed()
    }

    /**
     * 停止心跳机制。
     * 在连接断开时调用。
     */
    fun stop() {
        watchDogJob?.cancel()
        watchDogJob = null
    }

    /**
     * 启动或重启看门狗协程，在指定延迟后执行一次探测。
     */
    private fun schedule(delayMillis: Long) {
        watchDogJob?.cancel()
        watchDogJob = scope.launch {
            delay(delayMillis.milliseconds)
            runPulse()
        }
    }

    /**
     * 执行一次心跳探测。
     * 如果连续失败次数超过阈值，关闭会话；否则发送心跳请求并调度下一次探测。
     */
    private fun runPulse() {
        val times = barkTimes++

        if (times > retryTimes) {
            // 心跳失败次数超限，关闭会话
            logger.i("watchdog biting, pulse failed $times times, session invalid")
            val e = EasyException(
                ErrorCode.PULSE_TIME_OUT, ErrorType.CONNECT,
                "pulse time out", session.suffix
            )
            session.close(e)
        } else {
            val pulseRequest = socketClient.pulseRequest
            if (pulseRequest == null) {
                logger.w("no pulse request for watchdog")
            } else {
                logger.i("watchdog sending pulse")
                buildTask(pulseRequest, callback).execute()
                // 调度下一次探测
                schedule(retryIntervalMillis)
            }
        }
    }

    /**
     * 构建心跳任务。
     * 心跳任务类型为 [TaskType.PULSE]，不受连接等待影响。
     *
     * @param T 响应数据类型
     * @param request 心跳请求
     * @param callback 回调接口
     * @return 心跳任务实例
     */
    override fun <T> buildTask(request: Request<T>, callback: Callback<T>): Task<T> {
        return TaskImpl(
            TaskType.PULSE, socketClient.getTaskManager().generateTaskId(),
            request, callback, socketClient, session
        )
    }

    /** 心跳请求的回调，处理心跳响应结果 */
    private val callback: Callback<Boolean> = object : DefaultCallback<Boolean>() {
        override fun onSuccess(res: Boolean) {
            logger.i("pulse result: $res, type: request-response")
        }

        override fun onFailure(t: Throwable) {
            logger.i("pulse failed, type: request-response", t)
        }
    }

    /**
     * 处理服务器返回的心跳响应包（ping-pong 模式）。
     * 使用 [com.orientsec.easysocket.request.Decoder] 解码响应，仅记录日志，
     * 不重置计数器（计数器由 feed 重置）。
     *
     * @param packet 服务器返回的数据包
     */
    override fun handlePacket(packet: Packet) {
        val pulseDecoder = socketClient.pulseDecoder ?: return
        scope.launch {
            try {
                val success = withContext(options.codecDispatcher) {
                    pulseDecoder.decode(packet)
                }
                logger.i("pulse result: $success, type: ping-pong")
            } catch (t: Throwable) {
                logger.i("pulse decode failed, type: ping-pong", t)
            }
        }
    }
}
