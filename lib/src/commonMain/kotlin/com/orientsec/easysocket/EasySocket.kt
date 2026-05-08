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
 * The `EasySocket` class is a connection management utility for managing socket connections.
 * It provides initialization, connection handling, client management, and network state monitoring.
 */
object EasySocket {
    // A list containing all socket client instances.
    private val socketClients = mutableListOf<BaseSocketClient>()

    private val factory = ThreadFactory {
        Thread(it, "Easy-socket-main")
    }

    /**
     * 单线程线程池，方便处理异步计算
     */
    private val dispatcher: ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor(factory).asCoroutineDispatcher()

    // The coroutine scope used for library-wide operations.
    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    // The timestamp when the application entered the background.
    @Volatile
    private var backgroundTimestamp: Long = 0

    private var networkObserver: NetworkObserver? = null
    private var lifecycleObserver: AppLifecycleObserver? = null

    /**
     * Initializes the `EasySocket` instance.
     * @param networkObserver Platform-specific network observer.
     * @param lifecycleObserver Platform-specific app lifecycle observer.
     */
    fun initialize(
        networkObserver: NetworkObserver,
        lifecycleObserver: AppLifecycleObserver
    ) {
        if (this.networkObserver != null) {
            return // Already initialized
        }
        this.networkObserver = networkObserver
        this.lifecycleObserver = lifecycleObserver

        networkObserver.start {
            scope.launch { onNetworkAvailable() }
        }

        lifecycleObserver.start(object : AppLifecycleListener {
            override fun onForeground() {
                backgroundTimestamp = 0
            }

            override fun onBackground() {
                backgroundTimestamp = Platform.currentTimeMillis()
            }
        })
    }

    /**
     * Opens a new socket connection with the specified options.
     */
    fun open(options: Options): SocketClient {
        val socketClient = EasySocketClient(options, scope)
        addSocketClient(socketClient)
        return socketClient
    }

    fun addSocketClient(socketClient: BaseSocketClient) {
        synchronized(socketClients) {
            socketClients.add(socketClient)
        }
    }

    fun removeSocketClient(socketClient: BaseSocketClient) {
        synchronized(socketClients) {
            socketClients.remove(socketClient)
        }
    }

    /**
     * Shuts down all socket clients and releases resources.
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
        networkObserver = null
        lifecycleObserver = null
    }

    fun getBackgroundTimestamp(): Long = backgroundTimestamp

    private fun onNetworkAvailable() {
        val clients = synchronized(socketClients) { socketClients.toList() }
        for (socketClient in clients) {
            socketClient.onNetworkAvailable()
        }
    }

    fun isNetworkAvailable(): Boolean {
        return networkObserver?.isNetworkAvailable() ?: true
    }
}
