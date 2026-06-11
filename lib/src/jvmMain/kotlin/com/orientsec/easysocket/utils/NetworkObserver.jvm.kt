package com.orientsec.easysocket.utils

/**
 * JVM 平台的 [NetworkObserver] 实现。
 * 当前为占位实现，尚未完成具体功能。
 */
actual class NetworkObserver {
    /** 开始监听网络变化，空实现 */
    actual fun start(onAvailable: () -> Unit) {
    }

    /** 停止监听网络变化，空实现 */
    actual fun stop() {
    }

    /** 检查网络是否可用，尚未实现 */
    actual fun isNetworkAvailable(): Boolean {
        TODO("Not yet implemented")
    }
}