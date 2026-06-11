package com.orientsec.easysocket.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Android 平台的 [AppLifecycleObserver] 实现。
 *
 * 使用 AndroidX Lifecycle 的 [ProcessLifecycleOwner] 监听应用前后台切换事件。
 * 当应用进入前台时触发 [AppLifecycleListener.onForeground]，
 * 当应用进入后台时触发 [AppLifecycleListener.onBackground]。
 */
actual class AppLifecycleObserver {
    /** 应用生命周期监听器 */
    private var listener: AppLifecycleListener? = null

    /** Lifecycle 观察者，监听应用前后台切换 */
    private val observer = object : DefaultLifecycleObserver {
        /** 应用进入前台 */
        override fun onStart(owner: LifecycleOwner) {
            listener?.onForeground()
        }

        /** 应用进入后台 */
        override fun onStop(owner: LifecycleOwner) {
            listener?.onBackground()
        }
    }

    /**
     * 开始监听应用生命周期。
     * 注册 Lifecycle 观察者到 ProcessLifecycleOwner。
     *
     * @param listener 生命周期事件监听器
     */
    actual fun start(listener: AppLifecycleListener) {
        this.listener = listener
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    /**
     * 停止监听应用生命周期。
     * 移除 Lifecycle 观察者并清除引用。
     */
    actual fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        listener = null
    }
}