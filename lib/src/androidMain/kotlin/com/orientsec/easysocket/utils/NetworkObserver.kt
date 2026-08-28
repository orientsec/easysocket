package com.orientsec.easysocket.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission

/**
 * Android 平台的 [NetworkObserver] 实现。
 *
 * 使用 Android 的 [ConnectivityManager] 监听网络状态变化，
 * 当网络变为可用时通知回调。
 *
 * @param context Android 上下文，用于获取系统服务
 */
actual class NetworkObserver(private val context: Context) {
    /** 网络可用时的回调 */
    private var onAvailable: (() -> Unit)? = null

    /** 连接管理器 */
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** 网络状态变化回调 */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onAvailable?.invoke()
        }
    }

    /**
     * 开始监听网络状态变化。
     * 注册网络回调，当网络可用时触发 [onAvailable] 回调。
     *
     * @param onAvailable 网络可用时的回调
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    actual fun start(onAvailable: () -> Unit) {
        this.onAvailable = onAvailable
        val capability = NetworkCapabilities.NET_CAPABILITY_VALIDATED
        val request = NetworkRequest.Builder()
            .addCapability(capability)
            .build()
        cm.registerNetworkCallback(request, networkCallback)
    }

    /**
     * 停止监听网络状态变化。
     * 注销网络回调并清除引用。
     */
    actual fun stop() {
        cm.unregisterNetworkCallback(networkCallback)
        onAvailable = null
    }

    /**
     * 检查当前网络是否可用。
     * 使用 [NetUtils.isNetworkAvailable] 进行检查。
     *
     * @return true 如果网络可用
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    actual fun isNetworkAvailable(): Boolean {
        return NetUtils.isNetworkAvailable(context)
    }
}