package com.orientsec.easysocket;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.inner.EasyConnection;
import com.orientsec.easysocket.utils.NetUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 17:23
 * Author: Fredric
 * coding is art not science
 * <p>
 * 连接管理
 */
public class ConnectionManager {
    private int count;

    private Application application;

    //fix ConcurrentModificationException when Iterator
    private Set<EasyConnection<?>> connections = new CopyOnWriteArraySet<>();

    private final HandlerThread mHandlerThread;

    private static class InstanceHolder {
        static ConnectionManager INSTANCE = new ConnectionManager();
    }

    private ConnectionManager() {
        mHandlerThread = new HandlerThread("EasyConnectionManager");
        mHandlerThread.start();
    }

    @NonNull
    public static ConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 初始化，注册Activity生命周期监听及网络状态监听
     *
     * @param application 应用
     */
    void init(@NonNull Application application) {
        this.application = application;
        application.registerActivityLifecycleCallbacks(new EasySocketAppLifecycleListener());
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        ConnectivityManager cm = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkCallback(request, new NetworkCallbackImpl());

    }

    void addConnection(@NonNull EasyConnection<?> connection) {
        connections.add(connection);
    }

    public void removeConnection(@NonNull EasyConnection<?> connection) {
        connections.remove(connection);
    }

    public boolean isNetworkAvailable() {
        return NetUtils.isNetworkAvailable(application);
    }

    public Looper getMainLooper() {
        return mHandlerThread.getLooper();
    }

    /**
     * Activity生命周期监听。用于控制连接的前后台切换
     */
    private class EasySocketAppLifecycleListener implements Application.ActivityLifecycleCallbacks {


        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (count == 0) {
                for (EasyConnection<?> connection : connections) {
                    connection.setForeground();
                }
            }
            count++;
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            count--;
            if (count == 0) {
                for (EasyConnection<?> connection : connections) {
                    connection.setBackground();
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }

    }


    /**
     * 网络状态的广播监听
     */
    private class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            for (EasyConnection<?> connection : connections) {
                connection.onNetworkAvailable();
            }
        }
    }


}
