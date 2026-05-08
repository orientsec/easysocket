package com.orientsec.easysocket.session.ktor

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.Period
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.session.AbstractSession
import com.orientsec.easysocket.session.Reader
import com.orientsec.easysocket.session.Writer
import com.orientsec.easysocket.utils.Platform
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls
import kotlinx.coroutines.Dispatchers

/**
 * Ktor 端的 Session 实现。
 */
class KtorSession(
    socketClient: BaseSocketClient,
    address: Address,
    addressIndex: Int,
    id: Long
) : AbstractSession(socketClient, address, addressIndex, id) {

    private var mSocket: Socket? = null

    override suspend fun performConnect(): Boolean {
        logger.d("ktor connection is starting")
        try {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val startTimeMill = Platform.currentTimeMillis()
            var timestamp = startTimeMill

            // STEP 1: Connect
            var socket = aSocket(selectorManager).tcp().connect(address.host, address.port) {
                keepAlive = true
                noDelay = true
            }
            var currentTimeMillis = Platform.currentTimeMillis()
            connectTimeMap[Period.CONNECT] = currentTimeMillis - timestamp
            timestamp = currentTimeMillis

            // STEP 2: SSL
            if (address.isSsl) {
                socket = socket.tls(socketClient.scope.coroutineContext)
                currentTimeMillis = Platform.currentTimeMillis()
                connectTimeMap[Period.SSL] = currentTimeMillis - timestamp
                timestamp = currentTimeMillis
            }

            val connectTime = timestamp - startTimeMill
            connectTimeMap[Period.ALL] = connectTime
            logger.d("ktor connected in " + connectTime + "ms")

            this.mSocket = socket
            return true
        } catch (e: Exception) {
            logger.w("ktor connection start failed ", e)
            onFailed(
                EasyException(
                    ErrorCode.SOCKET_CONNECT,
                    ErrorType.CONNECT,
                    "ktor connection failed",
                    suffix,
                    e
                )
            )
            return false
        }
    }

    override fun getReader(): Reader {
        return KtorReader(this, socketClient, mSocket!!)
    }

    override fun getWriter(): Writer {
        return KtorWriter(this, socketClient.scope, mSocket!!)
    }

    override fun closeSocket() {
        mSocket?.close()
    }

    override val ipAddress: String? get() = null // Ktor 暂不方便获取 IP

}
