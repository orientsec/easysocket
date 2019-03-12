package com.orientsec.easysocket;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.orientsec.easysocket.inner.AbstractConnection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private List<AbstractConnection> connections = new CopyOnWriteArrayList<>();

    private static class InstanceHolder {
        static ConnectionManager INSTANCE = new ConnectionManager();
    }

    private ConnectionManager() {
    }

    public static ConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 初始化，注册Activity生命周期监听及网络状态监听
     *
     * @param application 应用
     */
    void init(Application application) {
        this.application = application;
        application.registerActivityLifecycleCallbacks(new EasySocketAppLifecycleListener());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        application.registerReceiver(new NetworkStateReceiver(), intentFilter);
    }

    void addConnection(AbstractConnection connection) {
        connections.add(connection);
        /*if (background) {
            connection.setBackground();
        } else {
            connection.setForeground();
        }*/
    }

    public void removeConnection(AbstractConnection connection) {
        connections.remove(connection);
    }

    /**
     * Activity生命周期监听。用于控制连接的前后台切换
     */
    private class EasySocketAppLifecycleListener implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public synchronized void onActivityStarted(Activity activity) {
            if (count == 0) {
                for (AbstractConnection connection : connections) {
                    connection.setForeground();
                }
            }
            count++;
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public synchronized void onActivityStopped(Activity activity) {
            count--;
            if (count == 0) {
                for (AbstractConnection connection : connections) {
                    connection.setBackground();
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }

    /**
     * 网络是否可用
     *
     * @return 网络是否可用
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取当前网络状态信息
        NetworkInfo info = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return info != null && info.isConnected();
    }

    /**
     * 网络状态监听器
     */
    public interface OnNetworkStateChangedListener {
        void onNetworkStateChanged(boolean available);
    }

    /**
     * 网络状态的广播监听
     */
    private class NetworkStateReceiver extends BroadcastReceiver {
        private boolean init;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!init) {
                init = true;
                return;
            }
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean isNetworkAvailable = isNetworkAvailable();
                for (AbstractConnection connection : connections) {
                    connection.onNetworkStateChanged(isNetworkAvailable);
                }
            }

        }
    }
}
