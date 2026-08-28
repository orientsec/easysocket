package com.orientsec.easysocket.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.orientsec.easysocket.EasySocket
import com.orientsec.easysocket.utils.AppLifecycleObserver
import com.orientsec.easysocket.utils.NetworkObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure EasySocket is initialized for Android
        EasySocket.initialize(NetworkObserver(applicationContext), AppLifecycleObserver())
        
        setContent {
            AppUI()
        }
    }
}
