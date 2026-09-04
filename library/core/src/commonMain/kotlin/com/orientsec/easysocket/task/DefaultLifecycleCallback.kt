package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet

/**
 * 默认的生命周期回调实现，所有方法均为空实现。
 * 可作为基类，按需重写需要的方法。
 *
 * @param T 回调结果的数据类型
 */
open class DefaultLifecycleCallback<T> : LifecycleCallback<T> {
    override fun onWait() {}

    override fun onResume() {}

    override fun onEncodeStart() {}

    override fun onEncodeSuccess() {}

    override fun onEncodeFailure(t: Throwable) {}

    override fun onReset(failedTimes: Int, t: Throwable) {}

    override fun onSendStart() {}

    override fun onSendSuccess() {}

    override fun onSendFailure(t: Throwable) {}

    override fun onPacketReceived(packet: Packet) {}

    override fun onDecodeStart() {}

    override fun onDecodeSuccess() {}

    override fun onDecodeFailure(t: Throwable) {}

    override fun onComplete() {}

    override fun onStart() {}

    override fun onSuccess(res: T) {}

    override fun onFailure(t: Throwable) {}

    override fun onCanceled() {}
}
