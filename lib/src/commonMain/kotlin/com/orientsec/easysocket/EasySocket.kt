package com.orientsec.easysocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EasySocket private constructor() {
    private val clients = mutableMapOf<String, SocketClient>()
    
    companion object {
        val instance = EasySocket()
    }

    fun open(options: Options, headParser: HeadParser): SocketClient {
        val client = KtorSocketClient(options, headParser)
        clients[options.name] = client
        return client
    }

    fun getClient(name: String): SocketClient? = clients[name]
}
