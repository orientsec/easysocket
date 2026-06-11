package com.orientsec.easysocket.socket

import com.orientsec.easysocket.Options

/**
 * Options.Builder 的扩展函数，配置使用传统 Java Socket 实现。
 *
 * 自动设置 [SocketSessionFactory] 作为会话工厂，
 * 并配置 [com.orientsec.easysocket.utils.AndroidTrafficProfiler] 作为流量统计器。
 *
 * 使用示例：
 * ```kotlin
 * val client = Options.build {
 *     name = "my-socket"
 *     isDebuggable = true
 *     useSocketDefaults()
 *     headParserProvider = { MyHeadParser() }
 *     addressList = listOf(Address("192.168.1.1", 8080))
 * }.open()
 * ```
 *
 * @return 当前 Builder 实例，支持链式调用
 */
fun Options.Builder.useSocketDefaults(): Options.Builder {
    this.sessionFactory = SocketSessionFactory()
    return this
}