package com.orientsec.easysocket

import android.app.Application
import com.orientsec.easysocket.session.ktor.KtorSessionFactory
import com.orientsec.easysocket.utils.AndroidTrafficProfiler
import com.orientsec.easysocket.utils.AppLifecycleObserver
import com.orientsec.easysocket.utils.NetworkObserver

/**
 * Android-specific initialization for EasySocket.
 */
fun EasySocket.initAndroid(application: Application) {
    initialize(
        networkObserver = NetworkObserver(application),
        lifecycleObserver = AppLifecycleObserver()
    )
}

/**
 * Helper method to configure Android-specific options using Ktor.
 */
fun Options.Builder.useAndroidDefaults(): Options.Builder {
    this.sessionFactory = KtorSessionFactory()
    this.trafficProfiler = AndroidTrafficProfiler()
    return this
}
