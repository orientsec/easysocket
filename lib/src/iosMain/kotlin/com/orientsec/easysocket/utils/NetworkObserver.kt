package com.orientsec.easysocket.utils

import platform.Network.*
import platform.darwin.dispatch_get_main_queue

/**
 * iOS 平台的 [NetworkObserver] 实现。
 * 使用 [NWPathMonitor] 监听网络状态。
 */
actual class NetworkObserver {
    private var monitor: nw_path_monitor_t? = null
    private var onAvailable: (() -> Unit)? = null
    private var lastStatusIsSatisfied = false

    /**
     * 开始监听网络状态变化。
     *
     * @param onAvailable 网络可用时的回调
     */
    actual fun start(onAvailable: () -> Unit) {
        this.onAvailable = onAvailable
        if (monitor != null) return

        val m = nw_path_monitor_create()
        monitor = m

        nw_path_monitor_set_update_handler(m) { path ->
            val status = nw_path_get_status(path)
            val isSatisfied = status == nw_path_status_satisfied
            if (isSatisfied && !lastStatusIsSatisfied) {
                this.onAvailable?.invoke()
            }
            lastStatusIsSatisfied = isSatisfied
        }

        nw_path_monitor_set_queue(m, dispatch_get_main_queue())
        nw_path_monitor_start(m)
    }

    /**
     * 停止监听网络状态变化。
     */
    actual fun stop() {
        monitor?.let {
            nw_path_monitor_cancel(it)
        }
        monitor = null
        onAvailable = null
    }

    /**
     * 检查当前网络是否可用。
     *
     * @return true 如果网络可用
     */
    actual fun isNetworkAvailable(): Boolean {
        return lastStatusIsSatisfied
    }
}
