package com.orientsec.easysocket.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

actual class AppLifecycleObserver {
    private var listener: AppLifecycleListener? = null
    
    private val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            listener?.onForeground()
        }

        override fun onStop(owner: LifecycleOwner) {
            listener?.onBackground()
        }
    }

    actual fun start(listener: AppLifecycleListener) {
        this.listener = listener
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    actual fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        listener = null
    }
}
