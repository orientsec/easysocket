package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

/**
 * 默认的生命周期回调实现，所有方法均为空实现。
 * 可作为基类，按需重写需要的方法。
 *
 * @param <T> 回调结果的数据类型
 */
public class DefaultLifecycleCallback<T> implements LifecycleCallback<T> {
    @Override
    public void onWait() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onEncodeStart() {

    }

    @Override
    public void onEncodeSuccess() {

    }

    @Override
    public void onEncodeFailure(Throwable t) {

    }

    @Override
    public void onSendStart() {

    }

    @Override
    public void onSendSuccess() {

    }

    @Override
    public void onSendFailure(Throwable t) {

    }

    @Override
    public void onPacketReceived(Packet packet) {

    }

    @Override
    public void onDecodeStart() {

    }

    @Override
    public void onDecodeSuccess() {

    }

    @Override
    public void onDecodeFailure(Throwable t) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onSuccess(@NonNull T res) {

    }

    @Override
    public void onFailure(@NonNull Throwable t) {

    }

    @Override
    public void onCanceled() {

    }
}
