package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.request.Request;

/**
 * Represents a task interface that can be executed, canceled, and queried for its state.
 *
 * @param <T> The type of the result returned by the task.
 */
public interface Task<T> {

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return The task ID.
     */
    int getTaskId();

    /**
     * Retrieves the type of the task.
     *
     * @return The type of the task, which can be one of the predefined TaskType values.
     */
    TaskType getTaskType();

    /**
     * Retrieves the data content of the task.
     *
     * @return A byte array representing the data, or null if no data is available.
     * Data is available after successful encoding.
     */
    @Nullable
    byte[] getData();

    /**
     * Retrieves the response object of the task.
     *
     * @return The response data, or null if no response is available.
     * Data is available after the task completes successfully.
     */
    @Nullable
    T getResponse();

    /**
     * Retrieves the exception that occurred during task execution.
     *
     * @return The exception object, or null if no error occurred.
     * This is available after the task fails.
     */
    @Nullable
    Throwable getError();

    /**
     * Asynchronously sends the request data to the server and notifies the callback
     * upon receiving a response, encountering an error, or processing the result.
     */
    void execute();

    /**
     * Cancels the current task. If the task is running, it attempts to interrupt execution.
     * If the task has not started, it marks the task as not to be executed.
     */
    void cancel();

    /**
     * Checks whether the task has been canceled.
     *
     * @return True if the task has been canceled, false otherwise.
     */
    boolean isCanceled();

    /**
     * Checks whether the task lifecycle has ended.
     *
     * @return True if the task is completed (either successfully or unsuccessfully),
     * false otherwise.
     */
    boolean isCompleted();

    /**
     * Checks whether the task was executed successfully.
     *
     * @return True if the task completed successfully, false otherwise.
     */
    boolean isSuccess();

    /**
     * Checks whether the task execution failed.
     *
     * @return True if an error occurred or the task was canceled, false otherwise.
     */
    boolean isFailure();

    /**
     * Retrieves the request object associated with the current task.
     *
     * @return A non-null request instance that persists throughout the task lifecycle.
     */
    @NonNull
    Request<T> request();
}