package com.orientsec.easysocket

/**
 * The `HeadParser` interface defines the structure for parsing data protocols.
 * It includes methods for obtaining the header size, parsing the header, and decoding the message
 * body.
 */
interface HeadParser {
    /**
     * Represents the header of a data packet.
     *
     * @property packetSize The size of the packet.
     */
    open class Head(val packetSize: Int)

    /**
     * Retrieves the length of the header.
     *
     * @return The length of the header.
     */
    fun headSize(): Int

    /**
     * Parses the header from the given raw byte data.
     *
     * @param bytes The raw byte data of the header.
     * @return A `Head` object containing the parsed header information.
     * @throws Exception If an error occurs during parsing.
     */
    @Throws(Exception::class)
    fun parseHead(bytes: ByteArray): Head

    /**
     * Decodes the message body using the provided header and body byte array.
     *
     * @param head      The parsed header.
     * @param bodyBytes The byte array of the message body.
     * @return A `Packet` object representing the decoded message body.
     * @throws Exception If an error occurs during decoding.
     */
    @Throws(Exception::class)
    fun decodePacket(head: Head, bodyBytes: ByteArray): Packet
}