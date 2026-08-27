package com.orientsec.easysocket.demo.client

import android.util.Log
import com.orientsec.easysocket.Address
import com.orientsec.easysocket.EasySocket
import com.orientsec.easysocket.Options
import com.orientsec.easysocket.SocketClient
import com.orientsec.easysocket.task.Callback

object Client {
    val socketClient: SocketClient
    val session: Session = Session()

    init {
        val address = Address("192.168.88.66", 10010)
        val addresses = listOf(address)
        val options = Options.build {
            isDebuggable = true
            minLogLevel = Log.DEBUG
            name = "EasySocketDemo"
            addressList = addresses
            headParserProvider = { MyHeadParser() }
            sessionInitializerProvider = { MySessionInitializer(this@Client) }
            requestTimeoutMills = 10000
            connectTimeoutMills = 5000
            connectIntervalMillis = 3000
            pulseDelaySeconds = 30
            backgroundActiveDurationSeconds = 20
        }
        socketClient = EasySocket.open(options)
    }

    fun request(param: String, callback: Callback<String>) {
        socketClient.buildTask(SimpleRequest(param, session), callback).execute()
    }
}
