package com.orientsec.easysocket.task;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.EasyException;

/**
 * Interface for managing tasks and their lifecycle in the EasySocket framework.
 * This interface extends `PacketHandler` and provides methods for generating task IDs,
 * resetting the task manager, and managing tasks (e.g., adding, removing, or canceling tasks).
 */
public interface TaskManager extends PacketHandler {

    /**
     * Generates a unique task ID to identify different tasks.
     *
     * @return The generated task ID as an integer.
     */
    int generateTaskId();

    /**
     * Resets the task manager.
     * This method is called on the main thread to reset the task manager's state
     * in case of an exception.
     *
     * @param e The exception that caused the reset.
     */
    @MainThread
    void reset(@NonNull EasyException e);

    /**
     * Prepares the task manager for operation.
     * This method is called on the main thread after resource initialization and login,
     * transitioning the task manager to a ready state.
     */
    @MainThread
    void ready();

    /**
     * Adds a task to the waiting queue.
     * This method is called on the main thread when a task cannot be executed immediately.
     *
     * @param task The task to be added to the waiting queue.
     */
    @MainThread
    void addTaskToWaitingQueue(@NonNull TaskImpl<?> task);

    /**
     * Adds a task to the task manager.
     * This method is called on the main thread to register a task for management.
     *
     * @param task The task to be added.
     */
    @MainThread
    void addTask(@NonNull TaskImpl<?> task);

    /**
     * Removes a task from the task manager.
     * This method is called on the main thread to delete a specific task from management.
     *
     * @param task The task to be removed.
     */
    @MainThread
    void removeTask(@NonNull Task<?> task);

    /**
     * Cancels a specific task.
     * This method is called on the main thread to cancel a task that is being managed.
     *
     * @param task The task to be canceled.
     */
    @MainThread
    void cancelTask(@NonNull TaskImpl<?> task);
}