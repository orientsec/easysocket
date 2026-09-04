package com.orientsec.easysocket.utils

/**
 * Interface for platform-specific traffic profiling.
 */
interface TrafficProfiler {
    /**
     * Sets a thread-local tag to be used for all sockets created on this thread.
     */
    fun setThreadStatsTag(tag: Int)

    /**
     * Clears any thread-local tag.
     */
    fun clearThreadStatsTag()

    /**
     * Tags the given socket so that all traffic on it is attributed to the current UID and tag.
     */
    fun tagSocket(socket: Any)

    /**
     * Untags the given socket.
     */
    fun untagSocket(socket: Any)
}

/**
 * No-op implementation of TrafficProfiler for platforms that don't support it.
 */
object NoTrafficProfiler : TrafficProfiler {
    override fun setThreadStatsTag(tag: Int) {}
    override fun clearThreadStatsTag() {}
    override fun tagSocket(socket: Any) {}
    override fun untagSocket(socket: Any) {}
}
