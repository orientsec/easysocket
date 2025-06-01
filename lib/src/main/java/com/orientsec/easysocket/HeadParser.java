package com.orientsec.easysocket;

import androidx.annotation.NonNull;

/**
 * The `HeadParser` interface defines the structure for parsing data protocols.
 * It includes methods for obtaining the header size, parsing the header, and decoding the message
 * body.
 */
public interface HeadParser {

    /**
     * Represents the header of a data packet.
     */
    class Head {
        /**
         * The size of the packet.
         */
        protected int packetSize;

        /**
         * Constructs a `Head` instance with the specified packet size.
         *
         * @param packetSize The size of the packet.
         */
        public Head(int packetSize) {
            this.packetSize = packetSize;
        }

        /**
         * Retrieves the size of the packet.
         *
         * @return The packet size.
         */
        public int getPacketSize() {
            return packetSize;
        }
    }

    /**
     * Retrieves the length of the header.
     *
     * @return The length of the header.
     */
    int headSize();

    /**
     * Parses the header from the given raw byte data.
     *
     * @param bytes The raw byte data of the header.
     * @return A `Head` object containing the parsed header information.
     * @throws Exception If an error occurs during parsing.
     */
    @NonNull
    Head parseHead(@NonNull byte[] bytes) throws Exception;

    /**
     * Decodes the message body using the provided header and body byte array.
     *
     * @param head      The parsed header.
     * @param bodyBytes The byte array of the message body.
     * @return A `Packet` object representing the decoded message body.
     * @throws Exception If an error occurs during decoding.
     */
    @NonNull
    Packet decodePacket(@NonNull Head head, @NonNull byte[] bodyBytes) throws Exception;

}