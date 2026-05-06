package com.orientsec.easysocket.task

import androidx.annotation.MainThread
import com.orientsec.easysocket.Packet

/**
 * OperableTask is an interface that extends the Task interface, providing additional
 * lifecycle methods for handling task operations such as resuming, receiving packets,
 * and handling errors or send events. All methods are annotated with @MainThread,
 * indicating they should be executed on the main thread.
 *
 * @param T The type of the result object handled by the task.
 */
interface OperableTask<T> : Task<T> {

    /**
     * Called when the task is resumed.
     * This method should be executed on the main thread.
     */
    fun onResume()

    /**
     * Called when a packet is received.
     * This method should be executed on the main thread.
     *
     * @param packet The packet received from the server.
     */
    fun onPacketReceived(packet: Packet)

    /**
     * Called when an error occurs during task execution to determine if the task should be reset.
     * This method should be executed on the main thread.
     *
     * @param t The throwable representing the error that occurred during task execution.
     * @return true if the task should be reset, false otherwise.
     */
    fun onReset(t: Throwable): Boolean

    /**
     * Called when the task starts sending data.
     * This method should be executed on the main thread.
     */
    fun onSendStart()

    /**
     * Called when the task successfully sends data.
     * This method should be executed on the main thread.
     */
    fun onSendSuccess()

    /**
     * Called when the task fails to send data.
     * This method should be executed on the main thread.
     *
     * @param t The throwable representing the failure.
     */
    fun onSendFailure(t: Throwable)
}
