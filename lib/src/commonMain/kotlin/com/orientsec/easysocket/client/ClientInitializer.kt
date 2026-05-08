package com.orientsec.easysocket.client

import com.orientsec.easysocket.Address

/**
 * The `ClientInitializer` interface defines the contract for initializing a client.
 * It provides a method to start the initialization process and an inner `Emitter` interface
 * for sending initialization results.
 */
interface ClientInitializer {

    /**
     * Starts the client initialization process.
     */
    suspend fun getAddressList(): Result<List<Address>>

}
