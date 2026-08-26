package com.orientsec.easysocket.session.ktor

import io.ktor.network.sockets.SocketAddress

/**
 * 从 Ktor [SocketAddress] 中提取底层 IP 地址字符串。
 *
 * 平台特定的扩展函数，JVM/Android 通过 toJavaAddress() 转为
 * InetSocketAddress 获取 hostAddress；其他平台按需实现。
 *
 * @return IP 地址字符串，无法获取时返回 null
 */
internal expect fun SocketAddress.extractIpAddress(): String?
