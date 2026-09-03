package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.SessionFactory
import javax.net.SocketFactory

/**
 * 基于传统 Java Socket 的会话工厂。
 *
 * 创建 [SocketSession] 实例，支持自定义 [SocketFactory]，
 * 可用于 SSL 连接等场景（传入 SSLSocketFactory）。
 *
 * 使用示例：
 * ```kotlin
 * val options = Options.build {
 *     useSocketDefaults()
 *     // 或使用 SSL：
 *     // sessionFactory = SocketSessionFactory(sslSocketFactory)
 *     headParserProvider = { MyHeadParser() }
 *     addressList = listOf(Address("host", 443, isSsl = true))
 * }
 * ```
 *
 * @param socketFactory Socket 工厂，默认使用系统默认工厂
 */
class SocketSessionFactory(
    private val socketFactory: SocketFactory = SocketFactory.getDefault()
) : SessionFactory {
    /**
     * 创建一个新的 [SocketSession] 实例。
     *
     * @param socketClient 所属的 Socket 客户端
     * @param address 连接的服务器地址
     * @param addressIndex 地址在列表中的索引
     * @param id 会话唯一标识
     * @return 新创建的 [SocketSession] 实例
     */
    override fun createSession(
        socketClient: BaseSocketClient,
        address: Address,
        addressIndex: Int,
        id: Long
    ): OperableSession {
        return SocketSession(socketClient, address, addressIndex, id, socketFactory)
    }
}