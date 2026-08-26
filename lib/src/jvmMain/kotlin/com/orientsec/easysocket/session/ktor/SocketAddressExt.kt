package com.orientsec.easysocket.session.ktor

import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.toJavaAddress
import java.net.InetSocketAddress

/**
 * JVM 平台实现：从 Ktor SocketAddress 提取 IP 地址字符串。
 */
internal actual fun SocketAddress.extractIpAddress(): String? {
    val javaAddress = toJavaAddress()
    return if (javaAddress is InetSocketAddress) {
        javaAddress.address?.hostAddress
    } else {
        null
    }
}
