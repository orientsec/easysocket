/**
 * 定义了EasySocket生命周期回调接口
 * 该接口扩展了Callback<R>，用于在Socket通信的不同阶段提供回调通知
 * 实现该接口的类将能够接收从等待连接开始到通信完成的各个阶段的通知
 */
package com.orientsec.easysocket.task;

import com.orientsec.easysocket.Packet;

/**
 * Defines the `LifecycleCallback` interface, which extends `Callback<T>`.
 * This interface provides lifecycle callbacks for various stages of task.
 *
 * @param <T> The type of the result expected from the callback.
 */
public interface LifecycleCallback<T> extends Callback<T> {

    /**
     * Called when the waiting for connection starts.
     * This method notifies the caller that the waiting process has begun.
     */
    void onWait();

    /**
     * Called when the waiting for connection ends.
     * This method notifies the caller that the waiting process has completed.
     */
    void onResume();

    /**
     * Called when data encoding starts.
     * This method notifies the caller that the data encoding process has started.
     * It may be triggered after the task ends.
     */
    void onEncodeStart();

    /**
     * Called when data encoding completes successfully.
     * This method notifies the caller that the data encoding process has finished.
     * It may be triggered after the task ends.
     */
    void onEncodeSuccess();

    /**
     * Called when data encoding fails.
     * This method notifies the caller that the data encoding process has encountered an error.
     *
     * @param t The exception containing error details.
     */
    void onEncodeFailure(Throwable t);

    /**
     * Called when data sending starts.
     * This method notifies the caller that the data sending process has started.
     * It may be triggered after the task ends.
     */
    void onSendStart();

    /**
     * Called when data sending completes successfully.
     * This method notifies the caller that the data sending process has finished.
     * It may be triggered after the task ends.
     */
    void onSendSuccess();

    /**
     * Called when data sending fails.
     * This method notifies the caller that the data sending process has encountered an error.
     *
     * @param t The exception containing error details.
     */
    void onSendFailure(Throwable t);

    /**
     * Called when a data packet is received.
     * This method notifies the caller that a data packet has been successfully received.
     *
     * @param packet The received data packet.
     */
    void onPacketReceived(Packet packet);

    /**
     * Called when data decoding starts.
     * This method notifies the caller that the data decoding process has started.
     * It may be triggered after the task ends.
     */
    void onDecodeStart();

    /**
     * Called when data decoding completes successfully.
     * This method notifies the caller that the data decoding process has finished.
     * It may be triggered after the task ends.
     */
    void onDecodeSuccess();

    /**
     * Called when data decoding fails.
     * This method notifies the caller that the data decoding process has encountered an error.
     *
     * @param t The exception containing error details.
     */
    void onDecodeFailure(Throwable t);

    /**
     * Called when the entire communication process is complete.
     * This method notifies the caller that the communication process has ended.
     */
    void onComplete();
}