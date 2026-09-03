package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.SessionFactory
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t

/**
 * 基于 iOS Network.framework (NWConnection) 的会话工厂。
 */
class NWSessionFactory : SessionFactory {
    // 随工厂（即客户端）创建一次，重连复用，不随会话重建
    private val queue: dispatch_queue_t =
        dispatch_queue_create("com.orientsec.easysocket.nw", null)

    override fun createSession(
        socketClient: BaseSocketClient,
        address: Address,
        addressIndex: Int,
        id: Long
    ): OperableSession {
        return NWSession(socketClient, address, addressIndex, id, queue)
    }
}
