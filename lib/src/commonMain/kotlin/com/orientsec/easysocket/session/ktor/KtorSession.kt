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
        return executeConnect()
            .onSuccess {
                this.mSocket = it.socket
                this.mSelectorManager = it.selector
            }
            .onFailure {
                logger.w("ktor connection failed", it)
                onFailed(it as EasyException)
            }
            .map { true }
            .recover { false }
            .getOrThrow()
    }

    /**
     * 在协程 IO 调度器中执行连接逻辑，管理 SelectorManager 的生命周期。
     */
    private suspend fun executeConnect(): Result<SelectorWrapper> = withContext(Dispatchers.IO) {
        val selector = SelectorManager(Dispatchers.IO)
        val stopwatch = Stopwatch()

        // 1. TCP 连接阶段
        val tcpSocket = connectTcp(selector)
            .onFailure {
                try {
                    selector.close()
                } catch (e: Exception) {
                    logger.e("failed to close selector", e)
                }
                return@withContext Result.failure(it)
            }
            .getOrThrow()
        stopwatch.record(Period.CONNECT, connectTimeMap)

        val tlsSocket = if (address.isSsl) {
            handshakeTls(tcpSocket).onFailure {
                try {
                    tcpSocket.close()
                } catch (e: Exception) {
                    logger.e("failed to close socket", e)
                }
                try {
                    selector.close()
                } catch (e: Exception) {
                    logger.e("failed to close selector", e)
                }
                return@withContext Result.failure(it)
            }.getOrThrow()
        } else tcpSocket

        val total = stopwatch.recordTotal(connectTimeMap)
        logger.d("ktor connected${if (address.isSsl) " (SSL)" else ""} in ${total}ms")

        return@withContext Result.success(SelectorWrapper(tlsSocket, selector))
    }

    /**
     * 建立 TCP 连接。
     */
    private suspend fun connectTcp(selector: SelectorManager): Result<Socket> {
        return try {
            val socket = withTimeout(options.connectTimeoutMills.milliseconds) {
                aSocket(selector).tcp().connect(address.host, address.port) {
                    keepAlive = true
                    noDelay = true
                }
            }
            Result.success(socket)
        } catch (e: Exception) {
            val code = if (e is TimeoutCancellationException) ErrorCode.SOCKET_CONNECT_TIMEOUT
            else ErrorCode.SOCKET_CONNECT
            val ex = EasyException(code, ErrorType.CONNECT, e.message ?: "failed", suffix, e)
            Result.failure(ex)
        }
    }

    /**
     * 进行 TLS 握手。
     */
    private suspend fun handshakeTls(tcpSocket: Socket): Result<Socket> {
        return try {
            val socket = withTimeout(options.tlsTimeoutMills.milliseconds) {
                tcpSocket.tls(socketClient.scope.coroutineContext)
            }
            Result.success(socket)
        } catch (e: Exception) {
            val errorCode = if (e is TimeoutCancellationException) ErrorCode.TLS_TIMEOUT
            else ErrorCode.TLS_ERROR
            val ex = EasyException(errorCode, ErrorType.CONNECT, "TLS failed", suffix, e)
            Result.failure(ex)
        }
    }

    private class SelectorWrapper(val socket: Socket, val selector: SelectorManager)

    /**
     * 创建 Ktor 读取器。
     *
     * @return [KtorReader] 实例
     */
    override fun createReader(): Reader {
        return KtorReader(this, socketClient, mSocket!!)
    }

    /**
     * 创建 Ktor 写入器。
     *
     * @return [KtorWriter] 实例
     */
    override fun createWriter(): Writer {
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
     * 获取对端 IP 地址。
     */
    override val ipAddress: String? get() = mSocket?.remoteAddress?.extractIpAddress()

}