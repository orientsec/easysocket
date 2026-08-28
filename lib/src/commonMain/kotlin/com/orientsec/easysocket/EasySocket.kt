package com.orientsec.easysocket

import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.client.EasySocketClient
import com.orientsec.easysocket.utils.AppLifecycleListener
import com.orientsec.easysocket.utils.AppLifecycleObserver
import com.orientsec.easysocket.utils.NetworkObserver
import com.orientsec.easysocket.utils.Platform
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * EasySocket 核心入口对象，负责管理所有 Socket 客户端实例的生命周期。
 *
 * 主要职责：
 * 1. 初始化平台相关的网络观察者和生命周期观察者
 * 2. 创建和管理 Socket 客户端实例
 * 3. 监听网络状态变化，通知所有客户端
 * 4. 跟踪应用前后台状态，用于重连策略判断
 * 5. 提供全局关闭功能，释放所有资源
 *
 * 使用方式：
 * ```kotlin
 * // 1. 初始化（Android 平台）
 * EasySocket.initAndroid(application)
 * // 2. 创建客户端
 * val client = Options.build {
 *     useAndroidDefaults()
 *     headParserProvider = { MyHeadParser() }
 *     addressList = listOf(Address("host", 8080))
 * }.open()
 * // 3. 启动连接
 * client.start()
 * ```
 */
object EasySocket {
    /** 所有已注册的 Socket 客户端实例列表 */
    private val socketClients = mutableListOf<BaseSocketClient>()

    /** 线程工厂，为 EasySocket 主线程指定名称 */
    private val factory = ThreadFactory {
        Thread(it, "Easy-socket-main")
    }

    /**
     * 单线程线程池调度器，方便处理异步计算。
     * 所有客户端的核心操作都在此调度器上执行，确保线程安全。
     */
    private val dispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor(factory).asCoroutineDispatcher()

    /** 库级别的协程作用域，使用 SupervisorJob 确保子协程互不影响 */
    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        // 全局异常处理，防止协程泄露导致的崩溃
        println("EasySocket global exception: $throwable")
        throwable.printStackTrace()
    })

    /**
     * 应用进入后台的时间戳。
     * 值为 0 表示应用当前在前台，非 0 表示进入后台的时间点。
     * 用于 [ReconnectPolicy.ACTIVE] 策略下判断是否需要重连。
     */
    @Volatile
    private var backgroundTimestamp: Long = 0

    /** 平台相关的网络状态观察者 */
    private var networkObserver: NetworkObserver? = null

    /** 平台相关的应用生命周期观察者 */
    private var lifecycleObserver: AppLifecycleObserver? = null

    /**
     * 初始化 EasySocket 实例。
     * 必须在使用库之前调用此方法，通常在 Application.onCreate() 中调用。
     * 重复调用不会产生副作用。
     *
     * @param networkObserver 平台相关的网络观察者，用于监听网络状态变化
     * @param lifecycleObserver 平台相关的应用生命周期观察者，用于监听前后台切换
     */
    fun initialize(
        networkObserver: NetworkObserver,
        lifecycleObserver: AppLifecycleObserver
    ) {
        if (this.networkObserver != null) {
            return // 已初始化，直接返回
        }
        this.networkObserver = networkObserver
        this.lifecycleObserver = lifecycleObserver

        // 注册网络可用回调，当网络恢复时通知所有客户端
        networkObserver.start {
            scope.launch { onNetworkAvailable() }
        }

        // 注册前后台切换回调
        lifecycleObserver.start(object : AppLifecycleListener {
            /** 应用回到前台时，重置后台时间戳 */
            override fun onForeground() {
                backgroundTimestamp = 0
            }

            /** 应用进入后台时，记录时间戳 */
            override fun onBackground() {
                backgroundTimestamp = Platform.currentTimeMillis()
            }
        })
    }

    /**
     * 使用指定配置创建并打开一个新的 Socket 客户端连接。
     *
     * @param options Socket 连接配置选项
     * @return 新创建的 [SocketClient] 实例
     */
    fun open(options: Options): SocketClient {
        val socketClient = EasySocketClient(options, scope)
        addSocketClient(socketClient)
        return socketClient
    }

    /**
     * 注册一个 Socket 客户端实例。
     *
     * @param socketClient 要注册的客户端实例
     */
    fun addSocketClient(socketClient: BaseSocketClient) {
        synchronized(socketClients) {
            socketClients.add(socketClient)
        }
    }

    /**
     * 移除一个已注册的 Socket 客户端实例。
     *
     * @param socketClient 要移除的客户端实例
     */
    fun removeSocketClient(socketClient: BaseSocketClient) {
        synchronized(socketClients) {
            socketClients.remove(socketClient)
        }
    }

    /**
     * 关闭所有 Socket 客户端并释放资源。
     * 调用后所有客户端将不可用，需重新初始化才能使用。
     */
    @Synchronized
    fun shutdown() {
        synchronized(socketClients) {
            for (client in socketClients) {
                client.shutdown()
            }
            socketClients.clear()
        }
        networkObserver?.stop()
        lifecycleObserver?.stop()
        scope.cancel()
        dispatcher.close()
        networkObserver = null
        lifecycleObserver = null
    }

    /**
     * 获取应用进入后台的时间戳。
     * 返回 0 表示应用当前在前台。
     *
     * @return 后台时间戳，0 表示在前台
     */
    fun getBackgroundTimestamp(): Long = backgroundTimestamp

    /**
     * 当网络恢复可用时，通知所有活跃的客户端尝试重连。
     * 仅通知当前没有会话的客户端。
     */
    private fun onNetworkAvailable() {
        val clients = synchronized(socketClients) { socketClients.toList() }
        for (socketClient in clients) {
            socketClient.onNetworkAvailable()
        }
    }

    /**
     * 检查当前网络是否可用。
     * 如果未初始化网络观察者，默认返回 true。
     *
     * @return 网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        return networkObserver?.isNetworkAvailable() ?: true
    }
}