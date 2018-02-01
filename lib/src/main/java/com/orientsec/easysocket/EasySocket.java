package com.orientsec.easysocket;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.orientsec.easysocket.inner.blocking.SocketConnection;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/25 13:01
 * Author: Fredric
 * coding is art not science
 */

public class EasySocket {
    private static AtomicBoolean initialized = new AtomicBoolean();

    public static void init(@NonNull Application application) {
        if (!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("init only once!");
        }
        //EasySocket.context = application.getApplicationContext();
        ConnectionManager.getInstance().init(application);
    }

    public static Connection open(Options options) {
        SocketConnection connection = new SocketConnection(options);
        ConnectionManager.getInstance().addConnection(connection);
        return connection;
    }
}
