package com.orientsec.easysocket;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.orientsec.easysocket.impl.SocketConnection;
import com.orientsec.easysocket.utils.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    public static <T> Connection<T> open(Options<T> options) {
        SocketConnection<T> connection = new SocketConnection<>(options);
        ConnectionManager.getInstance().addConnection(connection);
        return connection;
    }

    static class Executor {
        static ScheduledExecutorService scheduledExecutor
                = Executors.newSingleThreadScheduledExecutor();

        static ExecutorService managerExecutor
                = new ThreadPoolExecutor(0, 4,
                30L, TimeUnit.SECONDS,
                new SynchronousQueue<>());

        static ExecutorService codecExecutor
                = new ThreadPoolExecutor(0, 4,
                60L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    static class DefaultPushHandler<T> implements PushHandler<T> {

        @Override
        public void handleMessage(Packet<T> packet) {
            Logger.i("unhandled push event");
        }
    }

    static class MainThreadExecutor implements java.util.concurrent.Executor {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    }
}
