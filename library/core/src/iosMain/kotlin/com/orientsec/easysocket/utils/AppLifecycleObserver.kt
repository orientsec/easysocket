package com.orientsec.easysocket.utils

import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

/**
 * iOS 平台的 [AppLifecycleObserver] 实现。
 * 使用 [NSNotificationCenter] 监听 UIApplication 通知。
 */
actual class AppLifecycleObserver {
    private var listener: AppLifecycleListener? = null
    private var foregroundObserver: Any? = null
    private var backgroundObserver: Any? = null

    /**
     * 开始监听应用生命周期。
     *
     * @param listener 生命周期事件监听器
     */
    actual fun start(listener: AppLifecycleListener) {
        this.listener = listener

        foregroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            null
        ) {
            this.listener?.onForeground()
        }

        backgroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidEnterBackgroundNotification,
            null,
            null
        ) {
            this.listener?.onBackground()
        }
    }

    /**
     * 停止监听应用生命周期。
     */
    actual fun stop() {
        foregroundObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        backgroundObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        foregroundObserver = null
        backgroundObserver = null
        listener = null
    }
}
