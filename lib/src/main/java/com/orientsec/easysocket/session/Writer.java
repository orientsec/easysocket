package com.orientsec.easysocket.session;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.orientsec.easysocket.task.OperableTask;

/**
 * Interface for a message writer, defining operations for submitting and canceling message writing
 * tasks.
 */
public interface Writer {

    /**
     * Submits a task for message writing.
     *
     * @param task The task to be submitted, must not be null.
     */
    @MainThread
    void submit(@NonNull OperableTask<?> task);

    /**
     * Cancels a previously submitted message writing task.
     *
     * @param task The task to be canceled.
     */
    @MainThread
    void cancel(OperableTask<?> task);
}
