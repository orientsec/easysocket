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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 心跳管理器，负责维护 Socket 连接的活跃状态。
 *
 * 定期向服务器发送心跳请求，并处理服务器的心跳响应。
 * 如果连续心跳失败次数超过 [Options.pulseMaxLostTimes]，则认为连接已断开，
 * 会自动关闭会话。
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
    /** 心跳最大连续丢失次数，超过后认为连接断开 */
    private val maxLostTimes: Int

    /** 心跳间隔时间（毫秒） */
    private val intervalMillis: Long

    /** 当前连续心跳丢失次数 */
    private var lostTimes = 0

    /** 协程作用域 */
    val scope: CoroutineScope = socketClient.scope

    /** 配置选项 */
    val options: Options = socketClient.options

    /** 日志记录器 */
    private val logger: Logger

    /** 心跳协程任务 */
    private var pulseJob: Job? = null

    init {
        this.intervalMillis = options.pulseIntervalSeconds * 1000L
        this.maxLostTimes = options.pulseMaxLostTimes
        logger = session.logger
    }

    /**
     * 启动心跳机制。
     * 在连接成功并完成初始化后调用。
     * 如果已有心跳任务在运行，会先停止再重新启动。
     */
    fun start() {
        stop() // 确保取消已有的心跳任务
        pulseJob = scope.launch {
            while (isActive) {
                delay(intervalMillis)
                runPulse()
            }
        }
    }

    /**
     * 停止心跳机制。
     * 在连接断开时调用。
     */
    fun stop() {
        pulseJob?.cancel()
        pulseJob = null
    }

    /**
     * 执行一次心跳。
     * 如果连续丢失次数超过阈值，关闭会话；否则发送心跳请求。
     */
    private fun runPulse() {
        val currentLostTimes = lostTimes++

        if (currentLostTimes > maxLostTimes) {
            // 心跳失败次数超限，关闭会话
            logger.i("pulse failed times up, session invalid")
            val e = EasyException(
                ErrorCode.PULSE_TIME_OUT, ErrorType.CONNECT,
                "pulse time out", session.suffix
            )
            session.close(e)
        } else {
            val pulseRequest = socketClient.pulseRequest
            if (pulseRequest == null) {
                logger.w("no pulse request")
            } else {
                buildTask(pulseRequest, callback).execute()
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
            logger.i("client pulse result: $res")
            if (res) {
                // 心跳成功，重置丢失计数
                scope.launch { lostTimes = 0 }
            }
        }

        override fun onFailure(t: Throwable) {
            logger.i("client pulse failed ", t)
        }
    }

    /**
     * 处理服务器返回的心跳响应包。
     * 使用 [Decoder] 解码响应，如果心跳成功则重置丢失计数。
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
                logger.i("server pulse result: $success")
                if (success) lostTimes = 0
            } catch (t: Throwable) {
                logger.i("server pulse decode failed ", t)
            }
        }
    }
}