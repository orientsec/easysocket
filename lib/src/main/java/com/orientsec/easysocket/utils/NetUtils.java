package com.orientsec.easysocket.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Utility class `NetUtils` provides methods for network-related operations.
 */
public class NetUtils {

    /**
     * Checks whether the network is available.
     *
     * @param context The context used to access system services.
     * @return `true` if the network is available, `false` otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        // Get the ConnectivityManager system service
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        // Check network status for devices running below Android M
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } else {
            // Check network capabilities for devices running Android M and above
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
    }
}