package com.orientsec.easysocket.task;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;

/**
 * OperableTask is an interface that extends the Task interface, providing additional
 * lifecycle methods for handling task operations such as resuming, receiving packets,
 * and handling errors or send events. All methods are annotated with @MainThread,
 * indicating they should be executed on the main thread.
 *
 * @param <T> The type of the result object handled by the task.
 */
public interface OperableTask<T> extends Task<T> {

    /**
     * Called when the task is resumed.
     * This method should be executed on the main thread.
     */
    @MainThread
    void onResume();

    /**
     * Called when a packet is received.
     * This method should be executed on the main thread.
     *
     * @param packet The packet received from the server.
     */
    @MainThread
    void onPacketReceived(Packet packet);

    /**
     * Called when an error occurs during task execution.
     * This method should be executed on the main thread.
     *
     * @param t The throwable representing the error.
     */
    @MainThread
    void onError(@NonNull Throwable t);

    /**
     * Called when the task starts sending data.
     * This method should be executed on the main thread.
     */
    @MainThread
    void onSendStart();

    /**
     * Called when the task successfully sends data.
     * This method should be executed on the main thread.
     */
    @MainThread
    void onSendSuccess();

    /**
     * Called when the task fails to send data.
     * This method should be executed on the main thread.
     *
     * @param t The throwable representing the failure.
     */
    @MainThread
    void onSendFailure(@NonNull Throwable t);
}