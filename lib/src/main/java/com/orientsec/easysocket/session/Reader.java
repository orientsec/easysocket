package com.orientsec.easysocket.session;

import com.orientsec.easysocket.HeadParser;

import java.io.IOException;

/**
 * Represents a reader interface for processing incoming messages.
 * This interface defines a method for reading and decoding messages.
 */
public interface Reader {
    /**
     * Reads and processes incoming messages.
     * This method is responsible for handling the decoding of message headers and packets.
     *
     * @throws IOException If an I/O error occurs during the reading process.
     * @throws Exception   If an error occurs during the decoding process, specifically
     *                     in {@link HeadParser#decodePacket(HeadParser.Head, byte[])}.
     */
    void read() throws Exception;
}