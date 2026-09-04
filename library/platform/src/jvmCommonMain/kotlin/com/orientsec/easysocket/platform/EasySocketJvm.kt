package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Options

/**
 * Options.Builder 的扩展函数，配置使用传统 Java Socket 实现。
 *
 * 自动设置 [SocketSessionFactory] 作为会话工厂。
 *
 * @return 当前 Builder 实例，支持链式调用
 */
actual fun Options.Builder.useSocketDefaults(): Options.Builder {
    this.sessionFactory = SocketSessionFactory()
    return this
}