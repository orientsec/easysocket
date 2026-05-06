package com.orientsec.easysocket.session

import androidx.annotation.MainThread
import com.orientsec.easysocket.task.OperableTask

/**
 * Interface for a message writer, defining operations for submitting and canceling message writing
 * tasks.
 */
interface Writer {

    /**
     * Submits a task for message writing.
     *
     * @param task The task to be submitted, must not be null.
     */
    fun submit(task: OperableTask<*>)

    /**
     * Cancels a previously submitted message writing task.
     *
     * @param task The task to be canceled.
     */
    fun cancel(task: OperableTask<*>)
}
