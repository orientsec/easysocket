package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.utils.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the TaskManager interface for managing tasks in the EasySocket framework.
 * This class provides functionality for generating unique task IDs, handling packets,
 * resetting the task manager, and managing tasks in a map and waiting queue.
 */
public class TaskManagerImpl implements TaskManager {
    // Atomic counter for generating unique task IDs
    private final AtomicInteger uniqueTaskId = new AtomicInteger(0);

    // Map to store active tasks with their task IDs as keys
    private final Map<Integer, TaskImpl<?>> taskMap = new HashMap<>();

    // Queue to store tasks that are waiting to be executed
    private final Queue<TaskImpl<?>> waitingQueue = new LinkedList<>();

    // Logger instance for logging task-related information
    private final Logger logger;

    /**
     * Constructs a TaskManagerImpl instance with the specified logger.
     *
     * @param logger The logger instance for logging messages.
     */
    public TaskManagerImpl(Logger logger) {
        this.logger = logger;
    }

    /**
     * Generates a unique task ID.
     *
     * @return A unique integer task ID.
     */
    @Override
    public int generateTaskId() {
        return uniqueTaskId.incrementAndGet();
    }

    /**
     * Handles a received packet by finding the corresponding task and passing the packet to it.
     *
     * @param packet The packet received from the server.
     */
    @Override
    public void handlePacket(@NonNull Packet packet) {
        TaskImpl<?> task = taskMap.remove(packet.getTaskId());
        if (task != null) {
            task.onPacketReceived(packet);
        } else {
            logger.w("task manager handlePacket: task not found, " + packet);
        }
    }

    /**
     * Resets the task manager by clearing all tasks and notifying them of an error.
     *
     * @param e The exception that caused the reset.
     */
    @Override
    public void reset(@NonNull EasyException e) {
        for (TaskImpl<?> task : taskMap.values()) {
            task.onError(e);
        }
        taskMap.clear();
        waitingQueue.clear();
    }

    /**
     * Prepares the task manager for operation by resuming all tasks in the waiting queue.
     */
    @Override
    public void ready() {
        TaskImpl<?> task;
        while ((task = waitingQueue.poll()) != null) {
            task.onResume();
        }
    }

    /**
     * Adds a task to the waiting queue.
     *
     * @param task The task to be added to the waiting queue.
     */
    @Override
    public void addTaskToWaitingQueue(@NonNull TaskImpl<?> task) {
        waitingQueue.add(task);
    }

    /**
     * Adds a task to the task manager for active management.
     *
     * @param task The task to be added.
     */
    @Override
    public void addTask(@NonNull TaskImpl<?> task) {
        taskMap.put(task.getTaskId(), task);
    }

    /**
     * Removes a task from the task manager.
     *
     * @param task The task to be removed.
     */
    @Override
    public void removeTask(@NonNull Task<?> task) {
        taskMap.remove(task.getTaskId());
    }

    /**
     * Cancels a task by removing it from both the task map and the waiting queue.
     * Logs the result of the cancellation.
     *
     * @param task The task to be canceled.
     */
    public void cancelTask(@NonNull TaskImpl<?> task) {
        boolean removeFromTaskMap = taskMap.remove(task.getTaskId()) != null;
        boolean removeFromWaitingQueue = waitingQueue.remove(task);

        logger.i("cancel task: " + task.getTaskId() +
                " removed from task map: " + removeFromTaskMap +
                " removed from waiting queue: " + removeFromWaitingQueue);
    }
}