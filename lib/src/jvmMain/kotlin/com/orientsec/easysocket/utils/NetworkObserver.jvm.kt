package com.orientsec.easysocket.utils

/**
 * JVM 平台的 [NetworkObserver] 实现。
 *
 * JVM 没有通用的网络状态监听机制，当前为占位实现：
 * 不监听网络变化（[start] 为空实现），始终视为网络可用。
 * "网络恢复触发重连"的能力在 JVM 平台上不可用。
 */
actual class NetworkObserver {
    /** 开始监听网络变化，JVM 上为空实现 */
    actual fun start(onAvailable: () -> Unit) {
    }

    /** 停止监听网络变化 */
    actual fun stop() {
    }

    /**
     * 检查网络是否可用。
     * JVM 上无法探测，始终返回 true。
     */
    actual fun isNetworkAvailable(): Boolean {
        return true
    }
}
