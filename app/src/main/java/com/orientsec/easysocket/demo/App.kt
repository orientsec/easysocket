package com.orientsec.easysocket.demo

import android.app.Application
import com.orientsec.easysocket.EasySocket
import com.orientsec.easysocket.EasySocket.initialize

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize(this)
    }
}
