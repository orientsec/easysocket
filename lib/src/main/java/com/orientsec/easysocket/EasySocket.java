package com.orientsec.easysocket;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.client.EasySocketClient;
import com.orientsec.easysocket.utils.NetUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The `EasySocket` class is a connection management utility for managing socket connections.
 * It provides initialization, connection handling, client management, and network state monitoring.
 */
public class EasySocket {
    // The application context used for registering lifecycle and network state listeners.
    private Application application;

    // A thread-safe set containing all socket client instances.
    private final Set<BaseSocketClient> socketClients = new CopyOnWriteArraySet<>();

    // The main thread executor used for scheduling tasks.
    private EasyExecutor mainExecutor;

    // The timestamp when the application entered the background.
    private volatile long backgroundTimestamp;

    /**
     * A static inner class for lazy initialization of the `EasySocket` singleton instance.
     */
    private static class InstanceHolder {
        private static final EasySocket easySocket = new EasySocket();
    }

    /**
     * Retrieves the singleton instance of `EasySocket`.
     *
     * @return The singleton instance of `EasySocket`.
     */
    public static EasySocket getInstance() {
        return InstanceHolder.easySocket;
    }

    /**
     * Private constructor to prevent external instantiation.
     */
    private EasySocket() {
    }

    /**
     * Initializes the `EasySocket` instance. Should be called during application startup.
     * Registers activity lifecycle and network state listeners.
     *
     * @param application The application context.
     * @throws IllegalStateException If `EasySocket` is already initialized.
     */
    public synchronized void initialize(@NonNull Application application) {
        if (this.application != null)
            throw new IllegalStateException("EasySocket has already initialized");
        this.application = application;
        mainExecutor = new EasyExecutor();
        register(application);
    }

    /**
     * Retrieves the application context.
     *
     * @return The application context.
     * @throws IllegalStateException If `EasySocket` is not initialized.
     */
    @NonNull
    public Context getContext() {
        if (application == null) {
            throw new IllegalStateException("EasySocket is not initialized");
        }
        return application;
    }

    /**
     * Opens a new socket connection with the specified options.
     *
     * @param options The connection options.
     * @return A new socket client instance.
     * @throws IllegalStateException If `EasySocket` is not initialized.
     */
    @NonNull
    public SocketClient open(Options options) {
        if (application == null) {
            throw new IllegalStateException("EasySocket is not initialized");
        }
        EasySocketClient socketClient = new EasySocketClient(options, mainExecutor);
        EasySocket.getInstance().addSocketClient(socketClient);
        return socketClient;
    }

    /**
     * Adds a socket client to the management list.
     *
     * @param socketClient The socket client to add.
     */
    public void addSocketClient(@NonNull BaseSocketClient socketClient) {
        socketClients.add(socketClient);
    }

    /**
     * Removes a socket client from the management list.
     *
     * @param socketClient The socket client to remove.
     */
    public void removeSocketClient(@NonNull BaseSocketClient socketClient) {
        socketClients.remove(socketClient);
    }

    /**
     * Registers activity lifecycle and network state listeners.
     *
     * @param application The application context.
     */
    private void register(Application application) {
        int capability;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            capability = NetworkCapabilities.NET_CAPABILITY_INTERNET;
        else capability = NetworkCapabilities.NET_CAPABILITY_VALIDATED;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(capability)
                .build();
        ConnectivityManager cm
                = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkCallback(request, new EasySocket.NetworkCallbackImpl());

        // 注册前后台状态监听
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());
    }

    /**
     * Retrieves the timestamp when the application entered the background.
     *
     * @return The background timestamp.
     */
    public long getBackgroundTimestamp() {
        return backgroundTimestamp;
    }

    /**
     * Called when the network becomes available. Notifies all socket clients.
     */
    private void onNetworkAvailable() {
        for (BaseSocketClient socketClient : socketClients) {
            socketClient.onNetworkAvailable();
        }
    }

    /**
     * Checks if the network is available.
     *
     * @return `true` if the network is available, `false` otherwise.
     */
    public boolean isNetworkAvailable() {
        return NetUtils.isNetworkAvailable(application);
    }

    /**
     * Lifecycle observer for monitoring app foreground and background transitions.
     */
    private class AppLifecycleObserver implements DefaultLifecycleObserver {

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            // 应用进入前台
            backgroundTimestamp = 0;
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            // 应用进入后台
            backgroundTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * A network state listener for detecting network connectivity changes.
     */
    private class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            mainExecutor.execute(EasySocket.this::onNetworkAvailable);
        }
    }
}