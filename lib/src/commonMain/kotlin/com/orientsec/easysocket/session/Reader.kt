package com.orientsec.easysocket.session

/**
 * Represents a reader interface for processing incoming messages.
 * This interface defines a method for reading and decoding messages.
 */
interface Reader {
    fun start()

    /**
     * Reads and processes incoming messages.
     * This method is responsible for handling the decoding of message headers and packets.
     *
     * @throws Exception   If an error occurs during the reading or decoding process.
     */
    @Throws(Exception::class)
    suspend fun read()

    fun shutdown()
}
