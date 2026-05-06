package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet

/**
 * Defines the `LifecycleCallback` interface, which extends `Callback<T>`.
 * This interface provides lifecycle callbacks for various stages of task.
 *
 * @param T The type of the result expected from the callback.
 */
interface LifecycleCallback<T> : Callback<T> {

    /**
     * Called when the waiting for connection starts.
     * This method notifies the caller that the waiting process has begun.
     */
    fun onWait()

    /**
     * Called when the waiting for connection ends.
     * This method notifies the caller that the waiting process has completed.
     */
    fun onResume()

    /**
     * Called when data encoding starts.
     * This method notifies the caller that the data encoding process has started.
     * It may be triggered after the task ends.
     */
    fun onEncodeStart()

    /**
     * Called when data encoding completes successfully.
     * This method notifies the caller that the data encoding process has finished.
     * It may be triggered after the task ends.
     */
    fun onEncodeSuccess()

    /**
     * Called when data encoding fails.
     * This method notifies the caller that the data encoding process has encountered an error.
     *
     * @param t The exception containing error details.
     */
    fun onEncodeFailure(t: Throwable)

    /**
     * Called when the task is reset.
     * This method notifies the caller that the task has been reset.
     *
     * @param failedTimes The number of times the task has failed.
     * @param t           The exception containing error details.
     */
    fun onReset(failedTimes: Int, t: Throwable)

    /**
     * Called when data sending starts.
     * This method notifies the caller that the data sending process has started.
     * It may be triggered after the task ends.
     */
    fun onSendStart()

    /**
     * Called when data sending completes successfully.
     * This method notifies the caller that the data sending process has finished.
     * It may be triggered after the task ends.
     */
    fun onSendSuccess()

    /**
     * Called when data sending fails.
     * This method notifies the caller that the data sending process has encountered an error.
     *
     * @param t The exception containing error details.
     */
    fun onSendFailure(t: Throwable)

    /**
     * Called when a data packet is received.
     * This method notifies the caller that a data packet has been successfully received.
     *
     * @param packet The received data packet.
     */
    fun onPacketReceived(packet: Packet)

    /**
     * Called when data decoding starts.
     * This method notifies the caller that the data decoding process has started.
     * It may be triggered after the task ends.
     */
    fun onDecodeStart()

    /**
     * Called when data decoding completes successfully.
     * This method notifies the caller that the data decoding process has finished.
     * It may be triggered after the task ends.
     */
    fun onDecodeSuccess()

    /**
     * Called when data decoding fails.
     * This method notifies the caller that the data decoding process has encountered an error.
     *
     * @param t The exception containing error details.
     */
    fun onDecodeFailure(t: Throwable)

    /**
     * Called when the entire communication process is complete.
     * This method notifies the caller that the communication process has ended.
     */
    fun onComplete()
}
