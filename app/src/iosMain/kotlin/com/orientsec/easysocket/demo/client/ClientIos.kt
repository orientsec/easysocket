package com.orientsec.easysocket.demo.client

import com.orientsec.easysocket.Options
import com.orientsec.easysocket.platform.useSocketDefaults

/**
 * iOS 平台使用原生 Network.framework 实现
 */
actual fun Options.Builder.setupSessionFactory(): Options.Builder {
    return useSocketDefaults()
}
