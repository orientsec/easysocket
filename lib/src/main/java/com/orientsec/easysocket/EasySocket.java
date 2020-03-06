package com.orientsec.easysocket;

import android.app.Application;

import com.orientsec.easysocket.impl.SocketConnection;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.annotations.NonNull;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/25 13:01
 * Author: Fredric
 * coding is art not science
 */

public class EasySocket {
    private static AtomicBoolean initialized = new AtomicBoolean();

    /**
     * 初始化，在应用启动时执行
     *
     * @param application 应用上下文
     */
    public static void init(@NonNull Application application) {
        if (!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("init only once!");
        }
        //EasySocket.context = application.getApplicationContext();
        ConnectionManager.getInstance().init(application);
    }

    /**
     * 创建一个{@link Connection}
     *
     * @param options 连接设置选项
     * @return 连接
     */
    @NonNull
    public static <T> Connection<T> open(@NonNull Options<T> options) {
        SocketConnection<T> connection = new SocketConnection<>(options);
        ConnectionManager.getInstance().addConnection(connection);
        return connection;
    }

}
