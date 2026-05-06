package com.orientsec.easysocket

/**
 * The `Address` class represents connection information for a server.
 * It includes the server's IP address or domain name and port number.
 */
data class Address(
    val host: String,
    val port: Int
)
