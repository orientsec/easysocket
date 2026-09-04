package com.orientsec.easysocket.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission

/**
 * Utility class `NetUtils` provides methods for network-related operations.
 */
object NetUtils {

    /**
     * Checks whether the network is available.
     *
     * @param context The context used to access system services.
     * @return `true` if the network is available, `false` otherwise.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @JvmStatic
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        // Get the ConnectivityManager system service
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        // Check network status for devices running below Android M
        // Check network capabilities for devices running Android M and above
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
