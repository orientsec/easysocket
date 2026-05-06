package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.utils.Logger
import java.util.concurrent.Executor

class LifecycleCallbackWrapper<T>(
    private val callback: Callback<T>,
    private val task: Task<*>,
    client: BaseSocketClient
) : LifecycleCallback<T> {

    private val executor: Executor
    private val logger: Logger = client.logger
    private val isDebuggable: Boolean

    init {
        val options = client.options
        this.executor = options.callbackExecutor
        this.isDebuggable = options.isDebuggable
    }

    override fun onStart() {
        if (isDebuggable) logger.d(prefix() + "onStart")
        executor.execute { callback.onStart() }
    }

    override fun onSuccess(res: T) {
        if (isDebuggable)
            logger.d(prefix() + "onSuccess, type: " + res!!::class.java.simpleName)
        executor.execute {
            callback.onSuccess(res)
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    override fun onFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onFailure, error: " + t.message)
        executor.execute {
            callback.onFailure(t)
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    override fun onCanceled() {
        if (isDebuggable) logger.d(prefix() + "onCanceled")
        executor.execute {
            callback.onCanceled()
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    override fun onWait() {
        if (isDebuggable) logger.d(prefix() + "onWait")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onWait() }
        }
    }

    override fun onResume() {
        if (isDebuggable) logger.d(prefix() + "onResume")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onResume() }
        }
    }

    override fun onEncodeStart() {
        if (isDebuggable) logger.d(prefix() + "onEncodeStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onEncodeStart() }
        }
    }

    override fun onEncodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onEncodeSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onEncodeSuccess() }
        }
    }

    override fun onEncodeFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onEncodeFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onEncodeFailure(t) }
        }
    }

    override fun onReset(failedTimes: Int, t: Throwable) {
        if (isDebuggable)
            logger.d(
                prefix() + "onReset, failedTimes: " + failedTimes
                        + ", error: " + t.message
            )
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onReset(failedTimes, t) }
        }
    }

    override fun onSendStart() {
        if (isDebuggable) logger.d(prefix() + "onSendStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onSendStart() }
        }
    }

    override fun onSendSuccess() {
        if (isDebuggable) logger.d(prefix() + "onSendSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onSendSuccess() }
        }
    }

    override fun onSendFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onSendFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onSendFailure(t) }
        }
    }

    override fun onPacketReceived(packet: Packet) {
        if (isDebuggable) logger.d(prefix() + "onPacketReceived, packet: " + packet)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onPacketReceived(packet) }
        }
    }

    override fun onDecodeStart() {
        if (isDebuggable) logger.d(prefix() + "onDecodeStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onDecodeStart() }
        }
    }

    override fun onDecodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onDecodeSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onDecodeSuccess() }
        }
    }

    override fun onDecodeFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onDecodeFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            executor.execute { lifecycleCallback.onDecodeFailure(t) }
        }
    }

    override fun onComplete() {
        if (isDebuggable) logger.d(prefix() + "onComplete")
    }

    private fun prefix(): String {
        return "Task " + task.taskId + ", type: " + task.taskType + ", "
    }
}
