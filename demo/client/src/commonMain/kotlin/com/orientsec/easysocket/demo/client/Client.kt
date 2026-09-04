package com.orientsec.easysocket.demo.client

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.EasySocket
import com.orientsec.easysocket.Options
import com.orientsec.easysocket.SocketClient
import com.orientsec.easysocket.task.Callback
import com.orientsec.easysocket.utils.Platform

object Client {
    val socketClient: SocketClient by lazy {
        val address = Address("192.168.89.168", 10010, isSsl = false)
        val addresses = listOf(address)
        val options = Options.build {
            isDebuggable = true
            minLogLevel = Platform.LogLevel.DEBUG
            name = "EasySocketDemo"
            addressList = addresses
            headParserProvider = { MyHeadParser() }
            sessionInitializerProvider = { MySessionInitializer(this@Client) }
            requestTimeoutMills = 10000
            connectTimeoutMills = 5000
            connectIntervalMillis = 3000
            pulseDelaySeconds = 30
            backgroundActiveDurationSeconds = 20
            setupSessionFactory()
        }
        EasySocket.open(options)
    }

    val session: Session = Session()

    fun request(param: String, callback: Callback<String>) {
        socketClient.buildTask(SimpleRequest(param, session), callback).execute()
    }
}

/**
 * 平台相关的 SessionFactory 配置
 */
expect fun Options.Builder.setupSessionFactory(): Options.Builder

