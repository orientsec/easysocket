package com.orientsec.easysocket.task

import com.orientsec.easysocket.PacketHandler
import com.orientsec.easysocket.error.EasyException

/**
 * Interface for managing tasks and their lifecycle in the EasySocket framework.
 * This interface extends `PacketHandler` and provides methods for generating task IDs,
 * resetting the task manager, and managing tasks (e.g., adding, removing, or canceling tasks).
 */
interface TaskManager : PacketHandler {

    /**
     * Generates a unique task ID to identify different tasks.
     *
     * @return The generated task ID as an integer.
     */
    fun generateTaskId(): Int

    /**
     * Resets the task manager.
     * This method is called on the main thread to reset the task manager's state
     * in case of an exception.
     *
     * @param e The exception that caused the reset.
     */
    fun reset(e: EasyException)

    /**
     * Prepares the task manager for operation.
     * This method is called on the main thread after resource initialization and login,
     * transitioning the task manager to a ready state.
     */
    fun ready()

    /**
     * Adds a task to the waiting queue.
     * This method is called on the main thread when a task cannot be executed immediately.
     *
     * @param task The task to be added to the waiting queue.
     */
    fun addTaskToWaitingQueue(task: TaskImpl<*>)

    /**
     * Adds a task to the task manager.
     * This method is called on the main thread to register a task for management.
     *
     * @param task The task to be added.
     */
    fun addTask(task: TaskImpl<*>)

    /**
     * Removes a task from the task manager.
     * This method is called on the main thread to delete a specific task from management.
     *
     * @param task The task to be removed.
     */
    fun removeTask(task: Task<*>)

    /**
     * Cancels a specific task.
     * This method is called on the main thread to cancel a task that is being managed.
     *
     * @param task The task to be canceled.
     */
    fun cancelTask(task: TaskImpl<*>)
}
