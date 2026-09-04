package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Options

/**
 * Options.Builder 的扩展函数，配置使用 iOS Network.framework 实现。
 *
 * @return 当前 Builder 实例，支持链式调用
 */
actual fun Options.Builder.useSocketDefaults(): Options.Builder {
    this.sessionFactory = NWSessionFactory()
    return this
}
