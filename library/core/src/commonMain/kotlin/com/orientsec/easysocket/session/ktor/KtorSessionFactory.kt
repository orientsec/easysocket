package com.orientsec.easysocket.session.ktor

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.SessionFactory

/**
 * Session factory that creates [KtorSession] instances.
 */
class KtorSessionFactory : SessionFactory {
    override fun createSession(
        socketClient: BaseSocketClient,
        address: Address,
        addressIndex: Int,
        id: Long
    ): OperableSession {
        return KtorSession(socketClient, address, addressIndex, id)
    }
}
