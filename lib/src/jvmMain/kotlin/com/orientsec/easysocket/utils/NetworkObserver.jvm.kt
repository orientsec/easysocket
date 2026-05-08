package com.orientsec.easysocket.utils

actual class NetworkObserver {
    actual fun start(onAvailable: () -> Unit) {
    }

    actual fun stop() {
    }

    actual fun isNetworkAvailable(): Boolean {
        TODO("Not yet implemented")
    }
}