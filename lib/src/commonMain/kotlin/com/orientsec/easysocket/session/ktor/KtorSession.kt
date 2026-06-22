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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * 基于 Ktor 网络引擎的 Session 实现。
 *
 * 使用 Ktor 的 Socket API 进行连接建立、数据读写。
 * 支持 TCP 连接和 TLS/SSL 加密连接。
 * 连接过程会记录各阶段（CONNECT、SSL）的耗时。
 *
 * @param socketClient 所属的 Socket 客户端
 * @param address 连接的服务器地址
 * @param addressIndex 地址在列表中的索引
 * @param id 会话唯一标识
 */
class KtorSession(
    socketClient: BaseSocketClient,
    address: Address,
    addressIndex: Int,
    id: Long
) : AbstractSession(socketClient, address, addressIndex, id) {

    /** 底层 Ktor Socket 实例 */
    private var mSocket: Socket? = null

    /** Ktor SelectorManager，管理 IO 选择器线程，需要在关闭时释放 */
    private var mSelectorManager: SelectorManager? = null

    /**
     * 执行具体的连接逻辑。
     * 使用 Ktor 的 aSocket API 建立 TCP 连接，如果需要则进行 TLS 握手。
     *
     * @return true 如果连接成功，false 如果连接失败
     */
    override suspend fun performConnect(): Boolean {
        logger.d("ktor connection is starting")
        val connectResult = withContext(Dispatchers.IO) {
            val selector = SelectorManager(Dispatchers.IO)
            val startTime = Platform.currentTimeMillis()
            var lastTime = startTime

            // 1. TCP 连接阶段
            val tcpSocket = try {
                withTimeout(options.connectTimeoutMills.milliseconds) {
                    aSocket(selector).tcp().connect(address.host, address.port) {
                        keepAlive = true
                        noDelay = true
                    }
                }
            } catch (e: Exception) {
                selector.close()
                val errorCode = if (e is TimeoutCancellationException) ErrorCode.SOCKET_CONNECT
                else ErrorCode.SOCKET_CONNECT
                return@withContext ConnectResult(error = e, errorCode = errorCode)
            }

            var now = Platform.currentTimeMillis()
            connectTimeMap[Period.CONNECT] = now - lastTime
            lastTime = now

            // 2. TLS 握手阶段
            if (address.isSsl) {
                try {
                    val tlsSocket = withTimeout(options.tlsTimeoutMills.milliseconds) {
                        tcpSocket.tls(socketClient.scope.coroutineContext)
                    }
                    now = Platform.currentTimeMillis()
                    connectTimeMap[Period.SSL] = now - lastTime
                    connectTimeMap[Period.ALL] = now - startTime
                    logger.d(
                        "ktor connected (SSL) in " +
                                "${Platform.currentTimeMillis() - startTime}ms"
                    )
                    ConnectResult(socket = tlsSocket, selector = selector)
                } catch (e: Exception) {
                    selector.close()
                    tcpSocket.close()
                    val errorCode = if (e is TimeoutCancellationException) ErrorCode.TLS_TIMEOUT
                    else ErrorCode.TLS_ERROR
                    ConnectResult(error = e, errorCode = errorCode)
                }
            } else {
                connectTimeMap[Period.ALL] = now - startTime
                logger.d("ktor connected in ${now - startTime}ms")
                ConnectResult(socket = tcpSocket, selector = selector)
            }
        }

        val error = connectResult.error
        if (error != null) {
            logger.w("ktor connection failed", error)
            onFailed(
                EasyException(
                    connectResult.errorCode,
                    ErrorType.CONNECT,
                    "ktor connection failed",
                    suffix,
                    error
                )
            )
            return false
        }

        this.mSocket = connectResult.socket
        this.mSelectorManager = connectResult.selector
        return true
    }

    private class ConnectResult(
        val socket: Socket? = null,
        val selector: SelectorManager? = null,
        val error: Exception? = null,
        val errorCode: Int = ErrorCode.SOCKET_CONNECT
    )

    /**
     * 创建 Ktor 读取器。
     *
     * @return [KtorReader] 实例
     */
    override fun getReader(): Reader {
        return KtorReader(this, socketClient, mSocket!!)
    }

    /**
     * 创建 Ktor 写入器。
     *
     * @return [KtorWriter] 实例
     */
    override fun getWriter(): Writer {
        return KtorWriter(this, socketClient.scope, mSocket!!)
    }

    /**
     * 关闭底层 Ktor Socket 和 SelectorManager。
     */
    override fun closeSocket() {
        val socket = mSocket
        val selector = mSelectorManager
        mSocket = null
        mSelectorManager = null

        socketClient.scope.launch(Dispatchers.IO) {
            try {
                socket?.close()
                selector?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 获取 IP 地址。
     * Ktor 暂不方便获取底层 IP 地址，返回 null。
     */
    override val ipAddress: String? get() = null

}