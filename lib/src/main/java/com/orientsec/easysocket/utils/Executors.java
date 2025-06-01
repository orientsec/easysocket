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

/**
 * Utility class `Executors` provides static methods for creating and managing thread pools.
 * Includes default executors for codec, connection, writing, and main thread tasks,
 * as well as methods for creating custom thread pools.
 */
public class Executors {

    // Executor for managing connection tasks
    private static Executor connectExecutor;

    // Executor for managing connection tasks
    private static Executor writeExecutor;

    // Executor for managing codec tasks
    private static Executor codecExecutor;

    // Executor for managing main thread tasks
    private static Executor mainThreadExecutor;

    /**
     * Creates a custom thread pool executor.
     *
     * @param threadPoolSize   The size of the thread pool.
     * @param threadNamePrefix The prefix for thread names.
     * @param timeoutSeconds   The idle timeout for threads in seconds.
     * @return The created thread pool executor.
     */
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

    /**
     * Retrieves the default codec task executor.
     * If not already created, initializes an executor with a thread pool size of 4.
     *
     * @return The default codec task executor.
     */
    public static synchronized Executor defaultCodecExecutor() {
        if (codecExecutor == null) {
            codecExecutor = createExecutor(4,
                    "EasySocket_codec_", 60L);
        }
        return codecExecutor;
    }

    /**
     * Retrieves the default connection task executor.
     * If not already created, initializes an executor with a thread pool size of 8.
     *
     * @return The default connection task executor.
     */
    public static synchronized Executor defaultConnectExecutor() {
        if (connectExecutor == null) {
            connectExecutor = createExecutor(8,
                    "EasySocket_connect_", 30L);
        }
        return connectExecutor;
    }

    /**
     * Retrieves the default write task executor.
     * If not already created, initializes an executor with a thread pool size of 8.
     *
     * @return The default write task executor.
     */
    public static synchronized Executor defaultWriteExecutor() {
        if (writeExecutor == null) {
            writeExecutor = createExecutor(8,
                    "EasySocket_write_", 30L);
        }
        return writeExecutor;
    }

    /**
     * Retrieves the default main thread task executor.
     * If not already created, initializes an executor based on a `Handler`.
     *
     * @return The default main thread task executor.
     */
    public static synchronized Executor defaultMainThreadExecutor() {
        if (mainThreadExecutor == null) {
            mainThreadExecutor = new Executor() {
                private final Handler handler = new Handler(Looper.getMainLooper());

                @Override
                public void execute(@NonNull Runnable command) {
                    handler.post(command);
                }
            };
        }
        return mainThreadExecutor;
    }

    /**
     * Custom thread factory for creating threads with a specified name prefix.
     */
    public static class EasyThreadFactory implements ThreadFactory {
        // Prefix for thread names
        private final String threadName;

        // Thread group
        private final ThreadGroup group;

        /**
         * Constructs an `EasyThreadFactory` instance.
         *
         * @param threadName The prefix for thread names.
         */
        public EasyThreadFactory(String threadName) {
            this.threadName = threadName;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
        }

        // Counter for thread numbering
        private final AtomicInteger threadNumber = new AtomicInteger(0);

        /**
         * Creates a new thread.
         *
         * @param r The task to be executed by the thread.
         * @return The created thread.
         */
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