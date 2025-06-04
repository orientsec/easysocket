package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.request.Request;

/**
 * Interface for building executable tasks from requests and callbacks.
 * This interface defines a method to create tasks that can be executed
 * to handle server requests and process their responses.
 */
public interface TaskBuilder {

    /**
     * Creates an executable task.
     *
     * @param request  The request to be sent to the server.
     * @param callback The callback to handle the result of the task.
     * @param <T>      The type of the result returned by the task.
     * @return An executable task.
     */
    @NonNull
    <T> Task<T> buildTask(@NonNull Request<T> request, @NonNull Callback<T> callback);
}
