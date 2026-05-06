package com.orientsec.easysocket

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.client.EasySocketClient
import com.orientsec.easysocket.utils.NetUtils
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * The `EasySocket` class is a connection management utility for managing socket connections.
 * It provides initialization, connection handling, client management, and network state monitoring.
 */
object EasySocket {
    // The application context used for registering lifecycle and network state listeners.
    private var application: Application? = null

    // A thread-safe set containing all socket client instances.
    private val socketClients = CopyOnWriteArraySet<BaseSocketClient>()

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

    /**
     * Initializes the `EasySocket` instance. Should be called during application startup.
     * Registers activity lifecycle and network state listeners.
     *
     * @param application The application context.
     * @throws IllegalStateException If `EasySocket` is already initialized.
     */
    @Synchronized
    fun initialize(application: Application) {
        if (this.application != null) {
            throw IllegalStateException("EasySocket has already initialized")
        }
        this.application = application
        register(application)
    }

    /**
     * Retrieves the application context.
     *
     * @return The application context.
     * @throws IllegalStateException If `EasySocket` is not initialized.
     */
    val context: Context
        get() {
            return application ?: throw IllegalStateException("EasySocket is not initialized")
        }

    /**
     * Opens a new socket connection with the specified options.
     *
     * @param options The connection options.
     * @return A new socket client instance.
     * @throws IllegalStateException If `EasySocket` is not initialized.
     */
    fun open(options: Options): SocketClient {
        if (application == null) {
            throw IllegalStateException("EasySocket is not initialized")
        }
        val socketClient = EasySocketClient(options, scope)
        addSocketClient(socketClient)
        return socketClient
    }

    /**
     * Adds a socket client to the management list.
     *
     * @param socketClient The socket client to add.
     */
    fun addSocketClient(socketClient: BaseSocketClient) {
        socketClients.add(socketClient)
    }

    /**
     * Removes a socket client from the management list.
     *
     * @param socketClient The socket client to remove.
     */
    fun removeSocketClient(socketClient: BaseSocketClient) {
        socketClients.remove(socketClient)
    }

    /**
     * Registers activity lifecycle and network state listeners.
     *
     * @param application The application context.
     */
    private fun register(application: Application) {
        val capability = NetworkCapabilities.NET_CAPABILITY_VALIDATED

        val request = NetworkRequest.Builder()
            .addCapability(capability)
            .build()
        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        cm.registerNetworkCallback(request, NetworkCallbackImpl())

        // 注册前后台状态监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    /**
     * Shuts down all socket clients and releases resources.
     */
    @Synchronized
    fun shutdown() {
        for (client in socketClients) {
            client.shutdown()
        }
        socketClients.clear()
        scope.cancel()
        application = null
    }

    /**
     * Retrieves the timestamp when the application entered the background.
     *
     * @return The background timestamp.
     */
    fun getBackgroundTimestamp(): Long {
        return backgroundTimestamp
    }

    /**
     * Called when the network becomes available. Notifies all socket clients.
     */
    private fun onNetworkAvailable() {
        for (socketClient in socketClients) {
            socketClient.onNetworkAvailable()
        }
    }

    /**
     * Checks if the network is available.
     *
     * @return `true` if the network is available, `false` otherwise.
     */
    fun isNetworkAvailable(): Boolean {
        return NetUtils.isNetworkAvailable(application)
    }

    /**
     * Lifecycle observer for monitoring app foreground and background transitions.
     */
    private class AppLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // 应用进入前台
            backgroundTimestamp = 0
        }

        override fun onStop(owner: LifecycleOwner) {
            // 应用进入后台
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * A network state listener for detecting network connectivity changes.
     */
    private class NetworkCallbackImpl : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            scope.launch { onNetworkAvailable() }
        }
    }


}
