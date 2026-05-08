package com.orientsec.easysocket.utils

import android.net.TrafficStats

/**
 * Android-specific implementation of TrafficProfiler using TrafficStats.
 */
class AndroidTrafficProfiler : TrafficProfiler {
    override fun setThreadStatsTag(tag: Int) {
        TrafficStats.setThreadStatsTag(tag)
    }

    override fun clearThreadStatsTag() {
        TrafficStats.clearThreadStatsTag()
    }

    override fun tagSocket(socket: Any) {
        if (socket is java.net.Socket) {
            TrafficStats.tagSocket(socket)
        } else if (socket is java.net.DatagramSocket) {
            TrafficStats.tagDatagramSocket(socket)
        }
    }

    override fun untagSocket(socket: Any) {
        if (socket is java.net.Socket) {
            TrafficStats.untagSocket(socket)
        } else if (socket is java.net.DatagramSocket) {
            TrafficStats.untagDatagramSocket(socket)
        }
    }
}
