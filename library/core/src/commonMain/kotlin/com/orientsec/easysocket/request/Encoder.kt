package com.orientsec.easysocket.request

/**
 * An interface for encoding data into a byte array format.
 */
interface Encoder {

    /**
     * Encodes the given sequence ID into a byte array.
     *
     * @param sequenceId The sequence ID to be encoded.
     * @return A byte array representing the encoded sequence ID.
     */
    fun encode(sequenceId: Int): ByteArray
}
