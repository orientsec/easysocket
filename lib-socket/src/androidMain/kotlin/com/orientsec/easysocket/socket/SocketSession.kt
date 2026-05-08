package com.orientsec.easysocket.socket

import android.system.Os.socket
import android.util.Log.e
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.ssl.SSLSocket

/**
 * 传统 Socket 端的 Session 实现。
 */
class SocketSession(
    socketClient: BaseSocketClient,
    address: Address,
    addressIndex: Int,
    id: Long,
    private val socketFactory: javax.net.SocketFactory = javax.net.SocketFactory.getDefault()
) : AbstractSession(socketClient, address, addressIndex, id) {

    private var mSocket: Socket? = null

    override suspend fun performConnect(): Boolean {
        logger.d("socket connection is starting")
        val socket = withContext(Dispatchers.IO) {
            val s = try {
                socketFactory.createSocket()
            } catch (e: Exception) {
                logger.w("socket connection start failed ", e)
                onFailed(
                    EasyException(
                        ErrorCode.SOCKET_CONNECT,
                        ErrorType.CONNECT,
                        "socket connection failed",
                        suffix,
                        e
                    )
                )
                return@withContext null
            }
            try {
                options.trafficProfiler.tagSocket(s)
                s.tcpNoDelay = true
                s.keepAlive = true
                s.setPerformancePreferences(1, 2, 0)

                val startTimeMill = Platform.currentTimeMillis()
                var timestamp = startTimeMill

                val socketAddress: SocketAddress = InetSocketAddress(address.host, address.port)
                connectTimeMap[Period.DNS] = Platform.currentTimeMillis() - timestamp
                timestamp = Platform.currentTimeMillis()

                s.connect(socketAddress, options.connectTimeoutMillis)
                connectTimeMap[Period.CONNECT] = Platform.currentTimeMillis() - timestamp
                timestamp = Platform.currentTimeMillis()

                if (s is SSLSocket) {
                    s.startHandshake()
                    connectTimeMap[Period.SSL] = Platform.currentTimeMillis() - timestamp
                    timestamp = Platform.currentTimeMillis()
                }

                connectTimeMap[Period.ALL] = timestamp - startTimeMill
                logger.d("socket connected in " + (timestamp - startTimeMill) + "ms")
                s
            } catch (e: Exception) {
                options.trafficProfiler.untagSocket(s)
                try {
                    s.close()
                } catch (_: Exception) {
                }
                throw e
            }
        }
        this.mSocket = socket
        return socket != null
    }

    override fun closeSocket() {
        val socket = mSocket
        if (socket != null) {
            options.trafficProfiler.untagSocket(socket)
            socketClient.scope.launch(Dispatchers.IO) {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun getReader(): Reader {
        return BlockingReader(this, socketClient, mSocket!!)
    }

    override fun getWriter(): Writer {
        return QueuedWriter(this, socketClient.scope, mSocket!!)
    }

    override val ipAddress: String? get() = mSocket?.inetAddress?.hostAddress

}
