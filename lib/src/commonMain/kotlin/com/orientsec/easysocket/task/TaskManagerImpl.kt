package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.utils.Logger

/**
 * Implementation of the TaskManager interface for managing tasks in the EasySocket framework.
 * This class provides functionality for generating unique task IDs, handling packets,
 * resetting the task manager, and managing tasks in a map and waiting queue.
 */
class TaskManagerImpl(private val logger: Logger) : TaskManager {
    // Counter for generating unique task IDs
    private var uniqueTaskId = 0

    // Map to store active tasks with their task IDs as keys
    private val taskMap: MutableMap<Int, TaskImpl<*>> = mutableMapOf()

    // Queue to store tasks that are waiting to be executed
    private val waitingQueue = ArrayDeque<TaskImpl<*>>()

    /**
     * Generates a unique task ID.
     */
    @Synchronized
    override fun generateTaskId(): Int {
        return ++uniqueTaskId
    }

    /**
     * Handles a received packet by finding the corresponding task and passing the packet to it.
     */
    @Synchronized
    override fun handlePacket(packet: Packet) {
        val task = taskMap.remove(packet.taskId)
        if (task != null) {
            task.onPacketReceived(packet)
        } else {
            logger.w("task manager handlePacket: task not found, $packet")
        }
    }

    /**
     * Resets the task manager by clearing all tasks and notifying them of an error.
     */
    @Synchronized
    override fun reset(e: EasyException) {
        val tasksToKeep: MutableMap<Int, TaskImpl<*>> = mutableMapOf()
        for ((key, task) in taskMap) {
            if (task.onReset(e)) {
                tasksToKeep[key] = task
            }
        }
        taskMap.clear()
        taskMap.putAll(tasksToKeep)

        waitingQueue.clear()
        waitingQueue.addAll(tasksToKeep.values)
    }

    /**
     * Prepares the task manager for operation by resuming all tasks in the waiting queue.
     */
    @Synchronized
    override fun ready() {
        while (waitingQueue.isNotEmpty()) {
            val task = waitingQueue.removeFirst()
            task.onResume()
        }
    }

    @Synchronized
    override fun addTaskToWaitingQueue(task: TaskImpl<*>) {
        waitingQueue.addLast(task)
    }

    @Synchronized
    override fun addTask(task: TaskImpl<*>) {
        taskMap[task.taskId] = task
    }

    @Synchronized
    override fun removeTask(task: Task<*>) {
        taskMap.remove(task.taskId)
    }

    @Synchronized
    override fun cancelTask(task: TaskImpl<*>) {
        val removeFromTaskMap = taskMap.remove(task.taskId) != null
        val removeFromWaitingQueue = waitingQueue.remove(task)

        logger.i(
            "cancel task: " + task.taskId +
                    " removed from task map: " + removeFromTaskMap +
                    " removed from waiting queue: " + removeFromWaitingQueue
        )
    }
}
