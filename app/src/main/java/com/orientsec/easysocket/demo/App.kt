package com.orientsec.easysocket.demo

import android.app.Application
import com.orientsec.easysocket.EasySocket.initialize
import com.orientsec.easysocket.utils.AppLifecycleObserver
import com.orientsec.easysocket.utils.NetworkObserver

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize(NetworkObserver(this), AppLifecycleObserver())
    }
}
