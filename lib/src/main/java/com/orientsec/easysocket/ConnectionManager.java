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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.inner.EventListener;
import com.orientsec.easysocket.inner.EventManager;
import com.orientsec.easysocket.inner.Events;
import com.orientsec.easysocket.inner.RealConnection;

import java.util.HashSet;
import java.util.Set;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/27 17:23
 * Author: Fredric
 * coding is art not science
 * <p>
 * 连接管理
 */
public class ConnectionManager implements EventListener {
    final Application application;
    final HandlerThread handlerThread;
    //ON_START的activity数量，大于0时应用处于前台。
    private int count;

    private final Set<RealConnection> connections = new HashSet<>();

    private final EventManager eventManager;

    ConnectionManager(Application application) {
        this.application = application;
        handlerThread = new HandlerThread("EasyManager");
        handlerThread.start();
        eventManager = new EventManager(handlerThread.getLooper());
        eventManager.addListener(this);
        register(application);
    }

    public void addConnection(@NonNull RealConnection connection) {
        connections.add(connection);
        //应用在后台运行，将连接置于后台。
        if (count == 0) connection.setBackground();
    }

    public void removeConnection(@NonNull RealConnection connection) {
        connections.remove(connection);
    }

    public void register(Application application) {
        application.registerActivityLifecycleCallbacks(
                new ConnectionManager.EasySocketAppLifecycleListener());
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        ConnectivityManager cm
                = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkCallback(request, new ConnectionManager.NetworkCallbackImpl());
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        switch (eventId) {
            case Events.FOREGROUND:
                foreground();
                break;
            case Events.BACKGROUND:
                background();
                break;
            case Events.NET_AVAILABLE:
                networkAvailable();
                break;
        }
    }

    private void foreground() {
        if (count == 0) {
            for (RealConnection connection : connections) {
                connection.setForeground();
            }
        }
        count++;
    }

    private void background() {
        count--;
        if (count == 0) {
            for (RealConnection connection : connections) {
                connection.setBackground();
            }
        }
    }

    private void networkAvailable() {
        for (RealConnection connection : connections) {
            connection.onNetworkAvailable();
        }
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
            eventManager.publish(Events.FOREGROUND);
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            eventManager.publish(Events.BACKGROUND);
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
            eventManager.publish(Events.NET_AVAILABLE);
        }
    }


}
