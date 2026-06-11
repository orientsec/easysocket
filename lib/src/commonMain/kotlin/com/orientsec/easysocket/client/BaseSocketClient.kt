package com.orientsec.easysocket.client

import com.orientsec.easysocket.*
import com.orientsec.easysocket.push.PushManager
import com.orientsec.easysocket.request.Decoder
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.SessionInitializer
import com.orientsec.easysocket.task.TaskManager
import com.orientsec.easysocket.utils.LogFactory
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.CoroutineScope

/**
 * Socket 客户端的抽象基类，提供核心结构和行为定义。
 *
 * 管理连接配置、任务管理器、日志、推送管理器等核心组件。
 * 同时实现 [ConnectionListener] 接口，作为会话事件的中间层，
 * 将事件转发给外部注册的监听器。
 *
 * @property options Socket 连接配置选项
 * @property scope 协程作用域，用于执行客户端操作
 */
abstract class BaseSocketClient(
    override val options: Options,
    override val scope: CoroutineScope
) : SocketClient, ConnectionListener {

    /** 日志后缀，用于标识当前客户端实例 */
    val suffix: String = "  Client[${options.name}]"

    /**
     * 当前活跃的会话实例。
     * 由子类实现，返回当前连接的会话。
     *
     * @return 当前会话，未连接时返回 null
     */
    abstract override val session: OperableSession?

    /** 日志记录器，根据配置创建 */
    override val logger: Logger = LogFactory.getLogger(options, suffix)

    /** 推送消息管理器，延迟初始化 */
    override val pushManager: PushManager<*, *>? by lazy {
        options.pushManagerProvider?.invoke(this)
    }

    /** 协议头解析器，延迟初始化 */
    val headParser: HeadParser by lazy {
        options.headParserProvider.invoke(this)
    }

    /** 客户端初始化器，延迟初始化 */
    val clientInitializer: ClientInitializer? by lazy {
        options.clientInitializerProvider?.invoke(this)
    }

    /** 会话初始化器，延迟初始化 */
    val sessionInitializer: SessionInitializer? by lazy {
        options.sessionInitializerProvider?.invoke(this)
    }

    /** 心跳响应解码器，延迟初始化 */
    val pulseDecoder: Decoder<Boolean>? by lazy {
        options.pulseDecoderProvider?.invoke(this)
    }

    /** 心跳请求，延迟初始化 */
    val pulseRequest: Request<Boolean>? by lazy {
        options.pulseRequestProvider?.invoke(this)
    }

    /**
     * 处理网络恢复事件。
     * 由子类实现，通常在活跃状态下触发重连。
     */
    abstract override fun onNetworkAvailable()

    /**
     * 获取任务管理器。
     * 由子类实现，返回管理请求任务的 [TaskManager] 实例。
     *
     * @return 任务管理器
     */
    abstract fun getTaskManager(): TaskManager
}