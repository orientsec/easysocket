package com.orientsec.easysocket.session

import com.orientsec.easysocket.Address
import com.orientsec.easysocket.client.BaseSocketClient

/**
 * Interface for creating session instances.
 */
interface SessionFactory {
    /**
     * Creates a new [OperableSession] instance.
     */
    fun createSession(
        socketClient: BaseSocketClient,
        address: Address,
        addressIndex: Int,
        id: Long
    ): OperableSession
}
