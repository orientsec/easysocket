package com.orientsec.easysocket.demo

import androidx.compose.ui.window.ComposeUIViewController
import com.orientsec.easysocket.EasySocket
import com.orientsec.easysocket.utils.AppLifecycleObserver
import com.orientsec.easysocket.utils.NetworkObserver
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Initialize EasySocket for iOS
    EasySocket.initialize(NetworkObserver(), AppLifecycleObserver())
    
    return ComposeUIViewController {
        AppUI()
    }
}
