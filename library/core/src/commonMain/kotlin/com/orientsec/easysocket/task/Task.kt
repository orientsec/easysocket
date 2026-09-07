package com.orientsec.easysocket.task

import com.orientsec.easysocket.request.Request

/**
 * Represents a task interface that can be executed, canceled, and queried for its state.
 *
 * @param T The type of the result returned by the task.
 */
interface Task<T> {

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return The task ID.
     */
    val taskId: Int

    /**
     * Retrieves the type of the task.
     *
     * @return The type of the task, which can be one of the predefined TaskType values.
     */
    val taskType: TaskType


    /**
     * Checks whether the task has been canceled.
     *
     * @return True if the task has been canceled, false otherwise.
     */
    val isCanceled: Boolean

    /**
     * Checks whether the task lifecycle has ended.
     *
     * @return True if the task is completed (either successfully or unsuccessfully),
     * false otherwise.
     */
    val isCompleted: Boolean

    /**
     * Checks whether the task was executed successfully.
     *
     * @return True if the task completed successfully, false otherwise.
     */
    val isSuccess: Boolean

    /**
     * Checks whether the task execution failed.
     *
     * @return True if an error occurred or the task was canceled, false otherwise.
     */
    val isFailure: Boolean

    /**
     * Retrieves the data content of the task.
     *
     * @return A byte array representing the data, or null if no data is available.
     * Data is available after successful encoding.
     */
    val data: ByteArray?

    /**
     * Retrieves the response object of the task.
     *
     * @return The response data, or null if no response is available.
     * Data is available after the task completes successfully.
     */
    val response: T?

    /**
     * Retrieves the exception that occurred during task execution.
     *
     * @return The exception object, or null if no error occurred.
     * This is available after the task fails.
     */
    val error: Throwable?

    /**
     * Suspend function that waits for the task to complete and returns the result.
     * Throws an exception if the task fails or is canceled.
     *
     * @return The response data of type [T].
     * @throws Throwable The exception that occurred during task execution.
     */
    suspend fun await(): T

    /**
     * Asynchronously sends the request data to the server.
     */
    fun execute()

    /**
     * Cancels the current task. If the task is running, it attempts to interrupt execution.
     * If the task has not started, it marks the task as not to be executed.
     */
    fun cancel()

    /**
     * Retrieves the request object associated with the current task.
     *
     * @return A non-null request instance that persists throughout the task lifecycle.
     */
    fun request(): Request<T>
}

/**
 * Executes the task and awaits its result.
 *
 * Throws if the task fails or is canceled; the caller's coroutine cancellation
 * cancels the task.
 *
 * @return The decoded response.
 */
suspend fun <T> Task<T>.send(): T {
    execute()
    return await()
}
