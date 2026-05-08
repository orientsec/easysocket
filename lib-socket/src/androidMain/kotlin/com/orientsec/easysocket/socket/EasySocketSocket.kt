package com.orientsec.easysocket.socket

import com.orientsec.easysocket.Options

/**
 * Extension to use the traditional Socket-based implementation.
 */
fun Options.Builder.useSocketDefaults(): Options.Builder {
    this.sessionFactory = SocketSessionFactory()
    return this
}
