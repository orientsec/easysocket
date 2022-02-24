package com.orientsec.easysocket.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Executors {

    private static Executor connectExecutor;
    private static Executor codecExecutor;
    private static Executor mainExecutor;

    public static Executor createExecutor(int threadPoolSize,
                                          String threadNamePrefix,
                                          long timeoutSeconds) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadPoolSize,
                threadPoolSize,
                timeoutSeconds > 0 ? timeoutSeconds : 0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new EasyThreadFactory(threadNamePrefix));
        executor.allowCoreThreadTimeOut(timeoutSeconds > 0);
        return executor;
    }

    public static synchronized Executor defaultCodecExecutor() {
        if (codecExecutor == null) {
            codecExecutor = createExecutor(4,
                    "EasySocket_codec_", 60L);
        }
        return codecExecutor;
    }

    public static synchronized Executor defaultConnectExecutor() {
        if (connectExecutor == null) {
            connectExecutor = createExecutor(8,
                    "EasySocket_connect_", 30L);
        }
        return connectExecutor;
    }

    public static synchronized Executor defaultMainExecutor() {
        if (mainExecutor == null) {
            mainExecutor = new Executor() {
                private final Handler handler = new Handler(Looper.getMainLooper());

                @Override
                public void execute(@NonNull Runnable command) {
                    handler.post(command);
                }
            };
        }
        return mainExecutor;
    }


    public static class EasyThreadFactory implements ThreadFactory {
        private final String threadName;
        private final ThreadGroup group;

        public EasyThreadFactory(String threadName) {
            this.threadName = threadName;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
        }

        private final AtomicInteger threadNumber = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    threadName + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
