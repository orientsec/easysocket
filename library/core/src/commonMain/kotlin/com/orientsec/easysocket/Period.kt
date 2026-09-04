package com.orientsec.easysocket

/**
 * Enum `Period` defines the different phases of a connection process.
 * Each phase represents a specific stage in the connection lifecycle,
 * used for tracking and analyzing connection performance.
 */
enum class Period {
    /**
     * Represents the DNS resolution phase.
     * This phase resolves the hostname to an IP address.
     */
    DNS,

    /**
     * Represents the socket connection phase.
     * This phase establishes a TCP connection to the server.
     */
    CONNECT,

    /**
     * Represents the SSL handshake phase.
     * This phase establishes a secure SSL/TLS connection.
     */
    SSL,

    /**
     * Represents all phases of the connection process.
     * Includes DNS resolution, socket connection, and SSL handshake.
     */
    ALL
}
