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
import kotlin.system.measureTimeMillis
import kotlin.time.measureTimedValue

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
     * 连接过程中会记录各阶段（DNS、CONNECT、SSL）的耗时。
     *
     * @return true 如果连接成功，false 如果连接失败
     */
    override suspend fun performConnect(): Boolean {
        logger.d("socket connection is starting")
        val startTimeInMills = Platform.currentTimeMillis()
        val connectResult = withContext(Dispatchers.IO) {
            // 1. DNS 解析
            val dnsResult = dns()
            if (dnsResult is ConnectResult.Failure) return@withContext dnsResult
            dnsResult as ConnectResult.Success
            connectTimeMap[Period.DNS] = dnsResult.timeInMills
            val socketAddress: SocketAddress = dnsResult.value

            // 2. TCP 连接阶段
            val socketResult = connect(socketAddress)
            if (socketResult is ConnectResult.Failure) return@withContext socketResult
            socketResult as ConnectResult.Success
            connectTimeMap[Period.CONNECT] = socketResult.timeInMills
            val socket = socketResult.value

            // 3. SSL 握手阶段
            if (address.isSsl) {
                ssl(socket).also {
                    if (it is ConnectResult.Success) {
                        connectTimeMap[Period.SSL] = it.timeInMills
                    }
                }
            } else {
                socketResult
            }
        }

        return when (connectResult) {
            is ConnectResult.Failure -> {
                logger.w("socket connection failed", connectResult.error)
                EasyException(
                    connectResult.errorCode,
                    ErrorType.CONNECT,
                    "socket connection failed",
                    suffix,
                    connectResult.error
                ).let { onFailed(it) }
                false
            }

            is ConnectResult.Success -> {
                logger.d(
                    "socket connected in " +
                            "${Platform.currentTimeMillis() - startTimeInMills}ms"
                )
                this.mSocket = connectResult.value
                true
            }
        }
    }

    private fun dns(): ConnectResult<SocketAddress> {
        // 1. DNS 解析
        return try {
            measureTimedValue {
                InetSocketAddress(address.host, address.port)
            }.let {
                ConnectResult.success(
                    value = it.value,
                    timeInMills = it.duration.inWholeMilliseconds
                )
            }
        } catch (e: Exception) {
            ConnectResult.failure(
                error = e,
                errorCode = ErrorCode.DNS_ANALYZE
            )
        }
    }

    // 2. TCP 连接阶段
    private fun connect(socketAddress: SocketAddress): ConnectResult<Socket> {
        val socket = try {
            socketFactory.createSocket()
        } catch (e: Exception) {
            return ConnectResult.failure(
                error = e,
                errorCode = ErrorCode.SOCKET_CREATE
            )
        }
        options.trafficProfiler.tagSocket(socket)
        socket.tcpNoDelay = true
        socket.keepAlive = true
        socket.setPerformancePreferences(1, 2, 0)
        return try {
            val timeInMills = measureTimeMillis {
                socket.connect(socketAddress, options.connectTimeoutMills)
            }
            ConnectResult.success(socket, timeInMills)
        } catch (e: Exception) {
            options.trafficProfiler.untagSocket(socket)
            try {
                socket.close()
            } catch (_: Exception) {
            }
            ConnectResult.failure(
                error = e,
                errorCode = ErrorCode.SOCKET_CONNECT
            )
        }
    }

    private fun ssl(socket: Socket): ConnectResult<Socket> {
        if (socket !is SSLSocket) {
            options.trafficProfiler.untagSocket(socket)
            try {
                socket.close()
            } catch (_: Exception) {
            }
            return ConnectResult.failure(
                error = IllegalStateException("Not an SSLSocket"),
                errorCode = ErrorCode.TLS_ERROR
            )
        }

        return try {
            socket.soTimeout = options.tlsTimeoutMills
            val timeInMills = measureTimeMillis {
                socket.startHandshake()
            }
            socket.soTimeout = 0
            ConnectResult.success(value = socket, timeInMills = timeInMills)
        } catch (e: Exception) {
            options.trafficProfiler.untagSocket(socket)
            try {
                socket.close()
            } catch (_: Exception) {
            }
            val errorCode = if (e is java.net.SocketTimeoutException) ErrorCode.TLS_TIMEOUT
            else ErrorCode.TLS_ERROR
            ConnectResult.failure(error = e, errorCode = errorCode)
        }
    }

    /**
     * 连接结果封装类。
     */
    private sealed class ConnectResult<out T> {
        class Success<T>(
            val value: T,
            val timeInMills: Long
        ) : ConnectResult<T>()


        class Failure(
            val error: Exception,
            val errorCode: Int,
        ) : ConnectResult<Nothing>()

        companion object {
            fun <T> success(
                value: T,
                timeInMills: Long
            ): Success<T> {
                return Success(value, timeInMills)
            }

            fun failure(error: Exception, errorCode: Int): Failure {
                return Failure(error, errorCode)
            }
        }
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
    override fun createReader(): Reader {
        return BlockingReader(this, socketClient, mSocket!!)
    }

    /**
     * 创建队列式写入器。
     *
     * @return [QueuedWriter] 实例
     */
    override fun createWriter(): Writer {
        return QueuedWriter(this, socketClient.scope, mSocket!!)
    }

    /**
     * 获取连接的 IP 地址。
     *
     * @return IP 地址字符串，未连接时返回 null
     */
    override val ipAddress: String? get() = mSocket?.inetAddress?.hostAddress

}