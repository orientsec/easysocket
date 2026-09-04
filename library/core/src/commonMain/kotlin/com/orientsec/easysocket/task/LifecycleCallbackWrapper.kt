package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.launch

/**
 * 生命周期回调的包装器，为 [Callback] 提供线程调度和日志记录功能。
 *
 * 主要职责：
 * 1. 将所有回调切换到 [com.orientsec.easysocket.Options.callbackDispatcher] 指定的调度器上执行
 * 2. 在调试模式下输出每个生命周期事件的日志
 * 3. 如果原始回调实现了 [LifecycleCallback]，则同时调用其扩展方法
 * 4. 在任务完成（成功/失败/取消）时自动调用 [onComplete]
 *
 * @param T 响应数据类型
 * @param callback 原始回调接口
 * @param task 关联的任务实例
 * @param client 所属的 Socket 客户端
 */
class LifecycleCallbackWrapper<T>(
    private val callback: Callback<T>,
    private val task: Task<*>,
    private val client: BaseSocketClient
) : LifecycleCallback<T> {

    /** 日志记录器 */
    private val logger: Logger = client.logger

    /** 是否启用调试模式 */
    private val isDebuggable: Boolean = client.options.isDebuggable

    /**
     * 将回调调度到 [com.orientsec.easysocket.Options.callbackDispatcher] 上执行。
     *
     * @param block 要在回调调度器上执行的代码块
     */
    private fun dispatch(block: () -> Unit) {
        client.apply {
            scope.launch(options.callbackDispatcher) {
                block()
            }
        }
    }

    /** 任务开始执行 */
    override fun onStart() {
        if (isDebuggable) logger.d(prefix() + "onStart")
        dispatch { callback.onStart() }
    }

    /** 任务成功完成 */
    override fun onSuccess(res: T) {
        if (isDebuggable)
            logger.d(prefix() + "onSuccess")
        dispatch {
            callback.onSuccess(res)
            // 如果回调实现了 LifecycleCallback，调用 onComplete
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    /** 任务执行失败 */
    override fun onFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onFailure, error: " + t.message)
        dispatch {
            callback.onFailure(t)
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    /** 任务被取消 */
    override fun onCanceled() {
        if (isDebuggable) logger.d(prefix() + "onCanceled")
        dispatch {
            callback.onCanceled()
            (callback as? LifecycleCallback<T>)?.onComplete()
        }
        onComplete()
    }

    /** 任务进入等待状态 */
    override fun onWait() {
        if (isDebuggable) logger.d(prefix() + "onWait")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onWait() }
        }
    }

    /** 任务从等待中恢复 */
    override fun onResume() {
        if (isDebuggable) logger.d(prefix() + "onResume")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onResume() }
        }
    }

    /** 数据编码开始 */
    override fun onEncodeStart() {
        if (isDebuggable) logger.d(prefix() + "onEncodeStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onEncodeStart() }
        }
    }

    /** 数据编码成功 */
    override fun onEncodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onEncodeSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onEncodeSuccess() }
        }
    }

    /** 数据编码失败 */
    override fun onEncodeFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onEncodeFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onEncodeFailure(t) }
        }
    }

    /** 任务重置 */
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

    /** 数据发送开始 */
    override fun onSendStart() {
        if (isDebuggable) logger.d(prefix() + "onSendStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onSendStart() }
        }
    }

    /** 数据发送成功 */
    override fun onSendSuccess() {
        if (isDebuggable) logger.d(prefix() + "onSendSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onSendSuccess() }
        }
    }

    /** 数据发送失败 */
    override fun onSendFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onSendFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onSendFailure(t) }
        }
    }

    /** 接收到响应数据包 */
    override fun onPacketReceived(packet: Packet) {
        if (isDebuggable) logger.d(prefix() + "onPacketReceived, packet: " + packet)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onPacketReceived(packet) }
        }
    }

    /** 数据解码开始 */
    override fun onDecodeStart() {
        if (isDebuggable) logger.d(prefix() + "onDecodeStart")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onDecodeStart() }
        }
    }

    /** 数据解码成功 */
    override fun onDecodeSuccess() {
        if (isDebuggable) logger.d(prefix() + "onDecodeSuccess")
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onDecodeSuccess() }
        }
    }

    /** 数据解码失败 */
    override fun onDecodeFailure(t: Throwable) {
        if (isDebuggable) logger.d(prefix() + "onDecodeFailure, error: " + t.message)
        (callback as? LifecycleCallback<T>)?.let { lifecycleCallback ->
            dispatch { lifecycleCallback.onDecodeFailure(t) }
        }
    }

    /** 任务完成（无论成功、失败还是取消） */
    override fun onComplete() {
        if (isDebuggable) logger.d(prefix() + "onComplete")
    }

    /**
     * 生成日志前缀，包含任务ID和类型信息。
     *
     * @return 日志前缀字符串
     */
    private fun prefix(): String {
        return "Task " + task.taskId + ", type: " + task.taskType + ", "
    }
}