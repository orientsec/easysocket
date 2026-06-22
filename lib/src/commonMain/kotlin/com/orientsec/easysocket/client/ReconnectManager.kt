package com.orientsec.easysocket.client

import com.orientsec.easysocket.session.Session
import kotlinx.coroutines.*

/**
 * 重连管理器，负责处理连接失败或断开后的自动重连逻辑。
 *
 * 根据配置的 [ReconnectPolicy][com.orientsec.easysocket.ReconnectPolicy] 和客户端活跃状态，
 * 决定是否需要重连以及何时重连。支持延迟重连和立即重连两种模式。
 *
 * @param socketClient 关联的 Socket 客户端实例
 */
internal class ReconnectManager(private val socketClient: EasySocketClient) {
    /** 重连策略，决定是否及何时自动重连 */
    private val reconnectPolicy = socketClient.options.reconnectPolicy

    /** 重连间隔时间（毫秒） */
    private val connectIntervalMillis = socketClient.options.connectIntervalMillis.toLong()

    /** 日志记录器 */
    private val logger = socketClient.logger

    /** 延迟重连的协程任务 */
    private var reconnectJob: Job? = null

    /**
     * 延迟重连。
     * 在会话连接失败或断开后调用，根据策略决定是否延迟重连。
     * 如果当前服务器不可用，会先切换到下一个服务器地址。
     *
     * @param session 当前失败的会话实例
     */
    fun delayedReconnect(session: Session) {
        // 如果当前会话的服务器不可用，切换到下一个服务器
        if (!session.isServerAvailable) {
            socketClient.switchServer()
        }
        // 根据重连策略判断是否需要重连
        if (reconnectPolicy.shouldReconnect(socketClient.isActive())) {
            stop() // 取消之前待执行的重连任务
            reconnectJob = socketClient.scope.launch {
                logger.i("restart after $connectIntervalMillis mill seconds...")
                delay(connectIntervalMillis)
                reconnect()
            }
        } else {
            logger.i(
                "restart not needed, policy is $reconnectPolicy, " +
                        "active is ${socketClient.isActive()}"
            )
        }
    }

    /**
     * 立即重连。
     * 根据重连策略判断是否需要重连，如果需要则立即启动连接。
     */
    fun reconnect() {
        if (reconnectPolicy.shouldReconnect(socketClient.isActive())) {
            socketClient.onStart() // 启动连接，但不更新活跃时间戳
        } else {
            logger.i("restart canceled...")
        }
    }

    /**
     * 停止待执行的重连任务。
     * 在新的重连任务启动前或客户端关闭时调用。
     */
    fun stop() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}