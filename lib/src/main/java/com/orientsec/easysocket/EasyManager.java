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

import com.orientsec.easysocket.inner.AbstractConnection;
import com.orientsec.easysocket.inner.EventListener;
import com.orientsec.easysocket.inner.EventManager;
import com.orientsec.easysocket.inner.Events;

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
public class EasyManager implements EventListener {
    final Application application;
    final HandlerThread handlerThread;
    //ON_START的activity数量，大于0时应用处于前台。
    private int count;

    private final Set<AbstractConnection> connections = new CopyOnWriteArraySet<>();

    private final EventManager eventManager;

    private volatile long backgroundTimestamp;

    EasyManager(Application application) {
        this.application = application;
        handlerThread = new HandlerThread("EasyManager");
        handlerThread.start();
        eventManager = new EventManager(handlerThread.getLooper());
        eventManager.addListener(this);
        register(application);
    }

    public void addConnection(@NonNull AbstractConnection connection) {
        connections.add(connection);
    }

    public void removeConnection(@NonNull AbstractConnection connection) {
        connections.remove(connection);
    }

    public void register(Application application) {
        application.registerActivityLifecycleCallbacks(
                new EasyManager.EasySocketAppLifecycleListener());
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        ConnectivityManager cm
                = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkCallback(request, new EasyManager.NetworkCallbackImpl());
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId == Events.NET_AVAILABLE) {
            networkAvailable();
        }
    }

    private void foreground() {
        if (count == 0) {
            backgroundTimestamp = 0;
        }
        count++;
    }

    private void background() {
        count--;
        if (count == 0) {
            backgroundTimestamp = System.currentTimeMillis();
        }
    }

    public long getBackgroundTimestamp() {
        return backgroundTimestamp;
    }

    private void networkAvailable() {
        for (AbstractConnection connection : connections) {
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
            foreground();
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            background();
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
