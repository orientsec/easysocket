package com.orientsec.easysocket.utils

/**
 * Interface to listen for application foreground/background events.
 */
interface AppLifecycleListener {
    fun onForeground()
    fun onBackground()
}

/**
 * Expected class for observing application lifecycle changes.
 */
expect class AppLifecycleObserver {
    fun start(listener: AppLifecycleListener)
    fun stop()
}
