package com.orientsec.easysocket.session

import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.*

/**
 * An abstract class that provides a looping mechanism for executing tasks using Coroutines.
 * This class manages the lifecycle of the loop within a [CoroutineScope].
 */
abstract class Looper(private val logger: Logger) {
    // Job for managing the loop coroutine
    private var job: Job? = null

    // The number of times the loop has executed
    var loopTimes: Long = 0
        private set

    // The error that caused the loop to stop, if any
    protected var error: Throwable? = null

    /**
     * Starts the loop within the provided [CoroutineScope].
     * If the loop is already running, this method does nothing.
     */
    @Synchronized
    open fun start(scope: CoroutineScope) {
        if (job == null || job?.isCompleted == true) {
            loopTimes = 0
            error = null
            job = scope.launch(Dispatchers.IO) {
                runLoop()
            }
            logger.d("${javaClass.simpleName} is starting")
        }
    }

    /**
     * The main loop logic executed in the coroutine.
     */
    private suspend fun runLoop() {
        try {
            beforeLoop()
            while (currentCoroutineContext().isActive) {
                runInLoopThread()
                loopTimes++
            }
        } catch (_: CancellationException) {
            logger.d("${javaClass.simpleName} was cancelled")
        } catch (t: Throwable) {
            error = t
            logger.w("${javaClass.simpleName} is shutting down by error ", t)
        } finally {
            withContext(NonCancellable) {
                loopFinish()
            }
        }
    }

    /**
     * Called before the loop starts.
     *
     * @throws Exception If an error occurs during setup.
     */
    @Throws(Exception::class)
    protected abstract fun beforeLoop()

    /**
     * The logic to be executed in each iteration of the loop.
     *
     * @throws Exception If an error occurs during execution.
     */
    @Throws(Exception::class)
    protected abstract suspend fun runInLoopThread()

    /**
     * Called when the loop finishes.
     */
    protected abstract fun loopFinish()

    /**
     * Stops the loop and cancels the coroutine.
     */
    @Synchronized
    fun shutdown() {
        job?.cancel()
        job = null
    }

    /**
     * Checks whether the loop is currently running.
     *
     * @return `true` if the loop is running, `false` otherwise.
     */
    fun isRunning(): Boolean {
        return job?.isActive == true
    }
}
