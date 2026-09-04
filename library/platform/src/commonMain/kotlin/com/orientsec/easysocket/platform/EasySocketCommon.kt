package com.orientsec.easysocket.platform

import com.orientsec.easysocket.Options

/**
 * Options.Builder 的扩展函数，配置使用平台默认的原生 Socket 实现。
 *
 * 在 Android 上使用 java.net.Socket，在 iOS 上使用 Network.framework。
 *
 * @return 当前 Builder 实例，支持链式调用
 */
expect fun Options.Builder.useSocketDefaults(): Options.Builder
