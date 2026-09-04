package com.orientsec.easysocket

/**
 * The `Address` class represents connection information for a server.
 * It includes the server's IP address or domain name and port number.
 * This class is immutable and implements `Serializable` and `Cloneable` interfaces.
 */
data class Address(
    /**
     * The IP address or domain name of the server.
     */
    val host: String,
    /**
     * The port number of the server.
     */
    val port: Int,
    /**
     * Whether to use SSL/TLS.
     */
    val isSsl: Boolean = true
)