package com.orientsec.easysocket

enum class ReconnectPolicy {
    NONE,
    ACTIVE,
    ALWAYS;

    fun shouldReconnect(active: Boolean): Boolean {
        return when (this) {
            NONE -> false
            ACTIVE -> active
            ALWAYS -> true
        }
    }
}
