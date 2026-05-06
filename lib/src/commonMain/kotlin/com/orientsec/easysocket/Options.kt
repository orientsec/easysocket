package com.orientsec.easysocket

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Represents the configuration options for the EasySocket library.
 */
class Options private constructor(
    val name: String,
    val debuggable: Boolean,
    val addressList: List<Address>,
    val requestTimeoutMillis: Long,
    val connectTimeoutMillis: Long,
    val pulseIntervalSeconds: Int,
    val pulseMaxLostTimes: Int,
    val connectionDispatcher: CoroutineDispatcher,
    val callbackDispatcher: CoroutineDispatcher
) {
    class Builder {
        private var name: String = ""
        private var debuggable: Boolean = false
        private var addressList: List<Address> = emptyList()
        private var requestTimeoutMillis: Long = 5000
        private var connectTimeoutMillis: Long = 5000
        private var pulseIntervalSeconds: Int = 60
        private var pulseMaxLostTimes: Int = 2
        private var connectionDispatcher: CoroutineDispatcher = Dispatchers.Default
        private var callbackDispatcher: CoroutineDispatcher = Dispatchers.Main

        fun name(name: String) = apply { this.name = name }
        fun debuggable(debuggable: Boolean) = apply { this.debuggable = debuggable }
        fun addressList(addressList: List<Address>) = apply { this.addressList = addressList }
        fun requestTimeoutMillis(timeout: Long) = apply { this.requestTimeoutMillis = timeout }
        fun connectTimeoutMillis(timeout: Long) = apply { this.connectTimeoutMillis = timeout }
        fun pulseIntervalSeconds(interval: Int) = apply { this.pulseIntervalSeconds = interval }
        fun pulseMaxLostTimes(times: Int) = apply { this.pulseMaxLostTimes = times }
        fun connectionDispatcher(dispatcher: CoroutineDispatcher) = apply { this.connectionDispatcher = dispatcher }
        fun callbackDispatcher(dispatcher: CoroutineDispatcher) = apply { this.callbackDispatcher = dispatcher }

        fun build(): Options {
            if (addressList.isEmpty()) throw IllegalArgumentException("Address list cannot be empty")
            return Options(
                name, debuggable, addressList, requestTimeoutMillis,
                connectTimeoutMillis, pulseIntervalSeconds, pulseMaxLostTimes,
                connectionDispatcher, callbackDispatcher
            )
        }
    }
}
