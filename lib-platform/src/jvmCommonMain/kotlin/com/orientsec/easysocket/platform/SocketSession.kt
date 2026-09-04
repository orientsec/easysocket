package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.Period
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.session.AbstractSession
import com.orientsec.easysocket.session.Reader
import com.orientsec.easysocket.session.Writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
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
 * **SSL 前提**：默认工厂创建的是普通 Socket，无法完成 TLS 握手。
 * 连接 `isSsl = true` 的地址时，必须注入 SSL 工厂：
 * ```kotlin
 * sessionFactory = SocketSessionFactory(javax.net.ssl.SSLSocketFactory.getDefault())
 * ```
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
     *
     * @return true 如果连接成功，false 如果连接失败
     */
    override suspend fun performConnect(): Boolean {
        logger.d("socket connection is starting")
        return executeConnect()
            .onSuccess { this.mSocket = it }
            .onFailure {
                logger.w("socket connection failed", it)
                onFailed(it as EasyException)
            }
            .map { true }
            .recover { false }
            .getOrThrow()
    }

    /**
     * 在协程 IO 调度器中执行连接逻辑。
     */
    private suspend fun executeConnect(): Result<Socket> = withContext(Dispatchers.IO) {
        val stopwatch = Stopwatch()

        // 1. DNS 解析
        val socketAddress = dns()
            .onFailure { return@withContext Result.failure(it) }
            .getOrThrow()
        stopwatch.record(Period.DNS, connectTimeMap)

        // 2. TCP 连接阶段
        val tcpSocket = connectTcp(socketAddress)
            .onFailure { return@withContext Result.failure(it) }
            .getOrThrow()
        stopwatch.record(Period.CONNECT, connectTimeMap)

        // 3. SSL 握手阶段
        val tlsSocket = if (address.isSsl) {
            handshakeTls(tcpSocket)
                .onFailure {
                    options.trafficProfiler.untagSocket(tcpSocket)
                    try {
                        tcpSocket.close()
                    } catch (e: Exception) {
                        logger.e("failed to close socket", e)
                    }
                    return@withContext Result.failure(it)
                }
                .getOrThrow()
        } else tcpSocket

        val total = stopwatch.recordTotal(connectTimeMap)
        logger.d("socket connected${if (address.isSsl) " (SSL)" else ""} in ${total}ms")

        return@withContext Result.success(tlsSocket)
    }

    /**
     * DNS 解析。
     * 显式调用 [java.net.InetAddress.getByName] 完成解析：
     * - [Period.DNS] 计时反映真实的解析耗时
     * - 解析失败归因为 [ErrorCode.DNS_ANALYZE]（若留给 connect() 处理，
     *   UnknownHostException 会被归为 SOCKET_CONNECT，无法区分）
     */
    private fun dns(): Result<SocketAddress> {
        return try {
            val inetAddress = InetAddress.getByName(address.host)
            Result.success(InetSocketAddress(inetAddress, address.port))
        } catch (e: Exception) {
            val ex = EasyException(
                ErrorCode.DNS_ANALYZE,
                ErrorType.CONNECT,
                "dns resolve failed: $e",
                suffix,
                e
            )
            Result.failure(ex)
        }
    }

    /**
     * 建立 TCP 连接。
     */
    private fun connectTcp(socketAddress: SocketAddress): Result<Socket> {
        val socket = try {
            socketFactory.createSocket()
        } catch (e: Exception) {
            val ex = EasyException(
                ErrorCode.SOCKET_CREATE, ErrorType.CONNECT,
                "socket create failed", suffix, e
            )
            return Result.failure(ex)
        }
        options.trafficProfiler.tagSocket(socket)
        socket.tcpNoDelay = true
        socket.keepAlive = true
        socket.setPerformancePreferences(1, 2, 0)

        return try {
            socket.connect(socketAddress, options.connectTimeoutMills)
            Result.success(socket)
        } catch (e: Exception) {
            options.trafficProfiler.untagSocket(socket)
            try {
                socket.close()
            } catch (_: Exception) {
            }
            val code = if (e is java.net.SocketTimeoutException) ErrorCode.SOCKET_CONNECT_TIMEOUT
            else ErrorCode.SOCKET_CONNECT
            val ex = EasyException(code, ErrorType.CONNECT, e.message ?: "failed", suffix, e)
            Result.failure(ex)
        }
    }

    /**
     * 进行 TLS 握手。
     */
    private fun handshakeTls(tcpSocket: Socket): Result<Socket> {
        if (tcpSocket !is SSLSocket) {
            val ex = EasyException(
                ErrorCode.TLS_ERROR, ErrorType.CONNECT,
                "Not an SSLSocket: address requires TLS but the injected " +
                        "SocketFactory does not create SSLSocket. " +
                        "Provide SocketSessionFactory(SSLSocketFactory...)",
                suffix
            )
            return Result.failure(ex)
        }

        return try {
            tcpSocket.soTimeout = options.tlsTimeoutMills
            tcpSocket.startHandshake()
            tcpSocket.soTimeout = 0
            Result.success(tcpSocket)
        } catch (e: Exception) {
            val errorCode = if (e is java.net.SocketTimeoutException) ErrorCode.TLS_TIMEOUT
            else ErrorCode.TLS_ERROR
            val ex = EasyException(errorCode, ErrorType.CONNECT, "TLS failed", suffix, e)
            Result.failure(ex)
        }
    }

    /**
     * 关闭底层 Java Socket。
     * 取消流量标记后在 IO 线程中关闭 Socket。
     */
    override fun closeSocket() {
        val socket = mSocket
        mSocket = null
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
