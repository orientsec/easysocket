package com.orientsec.easysocket;

/**
 * Enum `PacketType` defines the types of packets used in the EasySocket library.
 * Each packet type represents a specific category of communication between the client and server.
 */
public enum PacketType {

    /**
     * Represents a response message.
     * This type is used for handling responses to client requests.
     */
    RESPONSE,

    /**
     * Represents a push message.
     * This type is used for handling server-initiated messages sent to the client.
     */
    PUSH,

    /**
     * Represents a heartbeat message.
     * This type is used for maintaining the connection's liveliness.
     */
    PULSE;

}