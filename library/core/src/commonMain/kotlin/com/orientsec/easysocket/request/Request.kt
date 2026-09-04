package com.orientsec.easysocket.request

import com.orientsec.easysocket.Packet

/**
 * Represents a request sent to the server in the EasySocket framework.
 * This abstract class provides methods for encoding request data and decoding server responses.
 *
 * @param <T> The type of the response object returned after decoding.
 */
abstract class Request<T> : Encoder, Decoder<T> {

    /**
     * Encodes the request data into a byte array.
     * This method processes the request data, allowing for tasks such as
     * business data filling, validation, and data encoding.
     * The returned byte array must not have a size of 0.
     *
     * @param sequenceId The sequence ID associated with the request.
     * @return A non-empty byte array representing the encoded request data.
     */
    abstract override fun encode(sequenceId: Int): ByteArray

    /**
     * Decodes the server's response into the specified return type.
     * The server message is processed using [com.orientsec.easysocket.HeadParser.decodePacket]
     * to obtain a [Packet]. This method converts the [Packet.body]
     * into the desired return result, while also allowing for unified exception handling
     * and other business logic processing.
     *
     * @param packet The packet data received from the server.
     * @return The decoded response object of type T.
     */
    abstract override fun decode(packet: Packet): T
}
