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

    /**
     * 执行具体的连接逻辑。
     * 使用 Ktor 的 aSocket API 建立 TCP 连接，如果需要则进行 TLS 握手。
     *
     * @return true 如果连接成功，false 如果连接失败
     */
    override suspend fun performConnect(): Boolean {
        logger.d("ktor connection is starting")
        try {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val startTimeMill = Platform.currentTimeMillis()
            var timestamp = startTimeMill

            // 步骤 1: 建立 TCP 连接
            var socket = aSocket(selectorManager).tcp().connect(address.host, address.port) {
                keepAlive = true
                noDelay = true
            }
            var currentTimeMillis = Platform.currentTimeMillis()
            connectTimeMap[Period.CONNECT] = currentTimeMillis - timestamp
            timestamp = currentTimeMillis

            // 步骤 2: TLS 握手（如果需要）
            if (address.isSsl) {
                socket = socket.tls(socketClient.scope.coroutineContext)
                currentTimeMillis = Platform.currentTimeMillis()
                connectTimeMap[Period.SSL] = currentTimeMillis - timestamp
                timestamp = currentTimeMillis
            }

            // 记录总连接耗时
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
     * 关闭底层 Ktor Socket。
     */
    override fun closeSocket() {
        mSocket?.close()
    }

    /**
     * 获取 IP 地址。
     * Ktor 暂不方便获取底层 IP 地址，返回 null。
     */
    override val ipAddress: String? get() = null

}