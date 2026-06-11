package com.orientsec.easysocket

import android.app.Application
import com.orientsec.easysocket.session.ktor.KtorSessionFactory
import com.orientsec.easysocket.utils.AndroidTrafficProfiler
import com.orientsec.easysocket.utils.AppLifecycleObserver
import com.orientsec.easysocket.utils.NetworkObserver

/**
 * Android 平台的 EasySocket 初始化扩展函数。
 *
 * 使用 Android 平台特定的 [NetworkObserver] 和 [AppLifecycleObserver] 初始化 EasySocket。
 * 通常在 Application.onCreate() 中调用。
 *
 * 使用示例：
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         EasySocket.initAndroid(this)
 *     }
 * }
 * ```
 *
 * @param application Android Application 实例
 */
fun EasySocket.initAndroid(application: Application) {
    initialize(
        networkObserver = NetworkObserver(application),
        lifecycleObserver = AppLifecycleObserver()
    )
}

/**
 * Options.Builder 的 Android 平台扩展函数，配置使用 Ktor 引擎和 Android 流量统计。
 *
 * 自动设置 [KtorSessionFactory] 作为会话工厂，
 * 并配置 [AndroidTrafficProfiler] 作为流量统计器。
 *
 * @return 当前 Builder 实例，支持链式调用
 */
fun Options.Builder.useAndroidDefaults(): Options.Builder {
    this.sessionFactory = KtorSessionFactory()
    this.trafficProfiler = AndroidTrafficProfiler()
    return this
}