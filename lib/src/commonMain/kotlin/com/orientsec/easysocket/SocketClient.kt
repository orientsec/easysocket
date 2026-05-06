package com.orientsec.easysocket

import kotlinx.coroutines.flow.SharedFlow

interface SocketClient {
    val options: Options
    val connectionState: SharedFlow<ConnectionState>
    
    suspend fun connect()
    suspend fun disconnect()
    
    fun isConnected(): Boolean
    
    suspend fun send(data: ByteArray)
    
    suspend fun <T> request(request: Request<T>): T
}

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Disconnected(val error: Throwable? = null) : ConnectionState()
}
