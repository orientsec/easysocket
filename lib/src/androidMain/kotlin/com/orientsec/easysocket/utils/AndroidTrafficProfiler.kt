package com.orientsec.easysocket.utils

import android.net.TrafficStats

/**
 * Android 平台的 [TrafficProfiler] 实现。
 *
 * 使用 Android 的 [TrafficStats] API 标记 Socket 流量，
 * 便于在 Android 的网络流量统计中区分不同操作的流量。
 * 支持 TCP Socket 和 UDP DatagramSocket 的标记。
 */
class AndroidTrafficProfiler : TrafficProfiler {
    /**
     * 设置当前线程的流量统计标签。
     * 后续在此线程上创建的 Socket 都会被标记此标签。
     *
     * @param tag 流量统计标签
     */
    override fun setThreadStatsTag(tag: Int) {
        TrafficStats.setThreadStatsTag(tag)
    }

    /**
     * 清除当前线程的流量统计标签。
     */
    override fun clearThreadStatsTag() {
        TrafficStats.clearThreadStatsTag()
    }

    /**
     * 标记指定 Socket 的流量到当前线程标签。
     * 支持 [java.net.Socket] 和 [java.net.DatagramSocket]。
     *
     * @param socket 要标记的 Socket 实例
     */
    override fun tagSocket(socket: Any) {
        if (socket is java.net.Socket) {
            TrafficStats.tagSocket(socket)
        } else if (socket is java.net.DatagramSocket) {
            TrafficStats.tagDatagramSocket(socket)
        }
    }

    /**
     * 取消标记指定 Socket 的流量。
     * 支持 [java.net.Socket] 和 [java.net.DatagramSocket]。
     *
     * @param socket 要取消标记的 Socket 实例
     */
    override fun untagSocket(socket: Any) {
        if (socket is java.net.Socket) {
            TrafficStats.untagSocket(socket)
        } else if (socket is java.net.DatagramSocket) {
            TrafficStats.untagDatagramSocket(socket)
        }
    }
}