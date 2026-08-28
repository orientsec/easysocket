package com.orientsec.easysocket.session.ktor

import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.InetSocketAddress

internal actual fun SocketAddress.extractIpAddress(): String? {
    return (this as? InetSocketAddress)?.hostname
}
