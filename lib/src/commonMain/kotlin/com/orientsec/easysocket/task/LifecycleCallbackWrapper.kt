package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.launch

class LifecycleCallbackWrapper<T>(
    private val callback: Callback<T>,
    private val task: Task<*>,
    private val client: BaseSocketClient
) : LifecycleCallback<T> {

    private val logger: Logger = client.logger
    private val isDebuggable: Boolean = client.options.isDebuggable

    private fun dispatch(block: () -> Unit) {
        client.apply {
            scope.launch(options.callbackDispatcher) {
                block()
            }
        }
    }

    override fun onStart() {
        if (isDebuggable) logger.d(prefix() + "onStart")
        dispatch { callback.onStart() }
    }

    override fun onSuccess(res: T) {
        if (isDebuggable)
            logger.d(prefix() + "onSuccess")
        dispatch {
            callback.onSuccess(res)
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    override fun onFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onFailure, error: " + t.message)
        dispatch {
            callback.onFailure(t)
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    override fun onCanceled() {
        if (isDebuggable) logger.d(prefix() + "onCanceled")
        dispatch {
            callback.onCanceled()
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    override fun onWait() {
        if (isDebuggable) logger.d(prefix() + "onWait")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onWait() }
        }
    }

    override fun onResume() {
        if (isDebuggable) logger.d(prefix() + "onResume")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onResume() }
        }
    }

    override fun onEncodeStart() {
        if (isDebuggable) logger.d(prefix() + "onEncodeStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onEncodeStart() }
        }
    }

    override fun onEncodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onEncodeSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onEncodeSuccess() }
        }
    }

    override fun onEncodeFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onEncodeFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onEncodeFailure(t) }
        }
    }

    override fun onReset(failedTimes: Int, t: Throwable) {
        if (isDebuggable)
            logger.d(
                prefix() + "onReset, failedTimes: " + failedTimes
                        + ", error: " + t.message
            )
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onReset(failedTimes, t) }
        }
    }

    override fun onSendStart() {
        if (isDebuggable) logger.d(prefix() + "onSendStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onSendStart() }
        }
    }

    override fun onSendSuccess() {
        if (isDebuggable) logger.d(prefix() + "onSendSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onSendSuccess() }
        }
    }

    override fun onSendFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onSendFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onSendFailure(t) }
        }
    }

    override fun onPacketReceived(packet: Packet) {
        if (isDebuggable) logger.d(prefix() + "onPacketReceived, packet: " + packet)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onPacketReceived(packet) }
        }
    }

    override fun onDecodeStart() {
        if (isDebuggable) logger.d(prefix() + "onDecodeStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onDecodeStart() }
        }
    }

    override fun onDecodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onDecodeSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onDecodeSuccess() }
        }
    }

    override fun onDecodeFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onDecodeFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onDecodeFailure(t) }
        }
    }

    override fun onComplete() {
        if (isDebuggable) logger.d(prefix() + "onComplete")
    }

    private fun prefix(): String {
        return "Task " + task.taskId + ", type: " + task.taskType + ", "
    }
}
