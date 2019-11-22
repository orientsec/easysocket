package com.orientsec.easysocket.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetUtils {
    /**
     * 网络是否可用
     *
     * @return 网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取当前网络状态信息
        NetworkInfo info = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return info != null && info.isConnected();
    }
}
