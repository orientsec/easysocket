package com.orientsec.easysocket.socket

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
 * 基于传统 Java Socket 的 Session 实现。
 *
 * 使用 java.net.Socket 进行连接建立和数据读写。
 * 支持 TCP 连接和 SSL/TLS 加密连接（通过 SSLSocket）。
 * 连接过程会记录各阶段（DNS、CONNECT、SSL）的耗时，
 * 并使用 TrafficProfiler 标记 Socket 流量。
 *
 * @param socketClient 所属的 Socket 客户端
 * @param address 连接的服务器地址
 * @param addressIndex 地址在列表中的索引
 * @param id 会话唯一标识
 * @param socketFactory Socket 工厂，用于创建 Socket 实例，默认使用系统默认工厂
 */
class SocketSession(
    socketClient: BaseSocketClient,
    address: Address,
    addressIndex: Int,
    id: Long,
    private val socketFactory: javax.net.SocketFactory = javax.net.SocketFactory.getDefault()
) : AbstractSession(socketClient, address, addressIndex, id) {

    /** 底层 Java Socket 实例 */
    private var mSocket: Socket? = null

    /**
     * 执行具体的连接逻辑。
     * 使用传统 Java Socket API 建立 TCP 连接，如果需要则进行 SSL 握手。
     * 连接过程中会标记 Socket 流量并记录各阶段耗时。
     *
     * @return true 如果连接成功，false 如果连接失败
     */
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
                // 标记 Socket 流量
                options.trafficProfiler.tagSocket(s)
                // 配置 Socket 参数
                s.tcpNoDelay = true
                s.keepAlive = true
                s.setPerformancePreferences(1, 2, 0)

                val startTimeMill = Platform.currentTimeMillis()
                var timestamp = startTimeMill

                // 步骤 1: DNS 解析 + TCP 连接
                val socketAddress: SocketAddress = InetSocketAddress(address.host, address.port)
                connectTimeMap[Period.DNS] = Platform.currentTimeMillis() - timestamp
                timestamp = Platform.currentTimeMillis()

                s.connect(socketAddress, options.connectTimeoutMillis)
                connectTimeMap[Period.CONNECT] = Platform.currentTimeMillis() - timestamp
                timestamp = Platform.currentTimeMillis()

                // 步骤 2: SSL 握手（如果需要）
                if (s is SSLSocket) {
                    s.startHandshake()
                    connectTimeMap[Period.SSL] = Platform.currentTimeMillis() - timestamp
                    timestamp = Platform.currentTimeMillis()
                }

                // 记录总连接耗时
                connectTimeMap[Period.ALL] = timestamp - startTimeMill
                logger.d("socket connected in " + (timestamp - startTimeMill) + "ms")
                s
            } catch (e: Exception) {
                // 连接失败，取消流量标记并关闭 Socket
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

    /**
     * 关闭底层 Java Socket。
     * 取消流量标记后在 IO 线程中关闭 Socket。
     */
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

    /**
     * 创建阻塞式读取器。
     *
     * @return [BlockingReader] 实例
     */
    override fun getReader(): Reader {
        return BlockingReader(this, socketClient, mSocket!!)
    }

    /**
     * 创建队列式写入器。
     *
     * @return [QueuedWriter] 实例
     */
    override fun getWriter(): Writer {
        return QueuedWriter(this, socketClient.scope, mSocket!!)
    }

    /**
     * 获取连接的 IP 地址。
     *
     * @return IP 地址字符串，未连接时返回 null
     */
    override val ipAddress: String? get() = mSocket?.inetAddress?.hostAddress

}