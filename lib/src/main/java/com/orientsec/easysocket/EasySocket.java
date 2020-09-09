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

import com.orientsec.easysocket.client.AbstractSocketClient;
import com.orientsec.easysocket.client.EasySocketClient;
import com.orientsec.easysocket.client.EventListener;
import com.orientsec.easysocket.client.EventManager;

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
public class EasySocket implements EventListener {
    private static final int NET_AVAILABLE = 1;

    private Application application;
    private HandlerThread handlerThread;
    //ON_START的activity数量，大于0时应用处于前台。
    private int count;

    private final Set<AbstractSocketClient> socketClients = new CopyOnWriteArraySet<>();

    private EventManager eventManager;

    private volatile long backgroundTimestamp;

    private static class InstanceHolder {
        private static EasySocket easySocket = new EasySocket();
    }

    public static EasySocket getInstance() {
        return InstanceHolder.easySocket;
    }

    private EasySocket() {
    }

    /**
     * 初始化，在应用启动时执行。
     * 注册Activity生命周期监听及网络状态监听。
     *
     * @param application 应用上下文
     */
    public synchronized void initialize(@NonNull Application application) {
        if (this.application != null)
            throw new IllegalStateException("EasySocket has already initialized.");
        this.application = application;
        handlerThread = new HandlerThread("EasyManager");
        handlerThread.start();
        eventManager = newEventManager();
        eventManager.addListener(this);
        register(application);
    }

    @NonNull
    public EventManager newEventManager() {
        return new EventManager(handlerThread.getLooper());
    }

    @NonNull
    public Context getContext() {
        if (application == null) {
            throw new IllegalStateException("EasySocket is not initialized.");
        }
        return application;
    }

    @NonNull
    public SocketClient open(Options options) {
        if (application == null) {
            throw new IllegalStateException("EasySocket is not initialized.");
        }
        return new EasySocketClient(options);
    }

    public void addSocketClient(@NonNull AbstractSocketClient socketClient) {
        socketClients.add(socketClient);
    }

    public void removeSocketClient(@NonNull AbstractSocketClient socketClient) {
        socketClients.remove(socketClient);
    }

    private void register(Application application) {
        application.registerActivityLifecycleCallbacks(
                new EasySocket.EasySocketAppLifecycleListener());
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        ConnectivityManager cm
                = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkCallback(request, new EasySocket.NetworkCallbackImpl());
    }

    @Override
    public void onEvent(int eventId, @Nullable Object object) {
        if (eventId == NET_AVAILABLE) {
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
        for (AbstractSocketClient socketClient : socketClients) {
            socketClient.onNetworkAvailable();
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
            eventManager.publish(NET_AVAILABLE);
        }
    }


}
