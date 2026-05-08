package com.orientsec.easysocket.socket

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.SessionFactory
import javax.net.SocketFactory

/**
 * Android-specific session factory that creates [SocketSession] instances.
 */
class SocketSessionFactory(
    private val socketFactory: SocketFactory = SocketFactory.getDefault()
) : SessionFactory {
    override fun createSession(
        socketClient: BaseSocketClient,
        address: Address,
        addressIndex: Int,
        id: Long
    ): OperableSession {
        return SocketSession(socketClient, address, addressIndex, id, socketFactory)
    }
}
