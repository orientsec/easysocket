package com.orientsec.easysocket.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

actual class NetworkObserver(private val context: Context) {
    private var onAvailable: (() -> Unit)? = null
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onAvailable?.invoke()
        }
    }

    actual fun start(onAvailable: () -> Unit) {
        this.onAvailable = onAvailable
        val capability = NetworkCapabilities.NET_CAPABILITY_VALIDATED
        val request = NetworkRequest.Builder()
            .addCapability(capability)
            .build()
        cm.registerNetworkCallback(request, networkCallback)
    }

    actual fun stop() {
        cm.unregisterNetworkCallback(networkCallback)
        onAvailable = null
    }

    actual fun isNetworkAvailable(): Boolean {
        return NetUtils.isNetworkAvailable(context)
    }
}
