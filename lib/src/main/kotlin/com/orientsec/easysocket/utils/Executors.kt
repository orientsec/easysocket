package com.orientsec.easysocket.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.*

/**
 * Utility class `Executors` provides static methods for creating and managing thread pools.
 * Includes default executor for main thread tasks.
 */
object Executors {

    @get:Synchronized
    private var mainThreadExecutor: Executor? = null

    /**
     * Retrieves the default main thread task executor.
     * If not already created, initializes an executor based on a `Handler`.
     *
     * @return The default main thread task executor.
     */
    @JvmStatic
    @Synchronized
    fun defaultMainThreadExecutor(): Executor {
        if (mainThreadExecutor == null) {
            mainThreadExecutor = object : Executor {
                private val handler = Handler(Looper.getMainLooper())

                override fun execute(command: Runnable) {
                    handler.post(command)
                }
            }
        }
        return mainThreadExecutor!!
    }
}
