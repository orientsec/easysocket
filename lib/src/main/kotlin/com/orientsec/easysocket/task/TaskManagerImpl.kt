package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.utils.Logger
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of the TaskManager interface for managing tasks in the EasySocket framework.
 * This class provides functionality for generating unique task IDs, handling packets,
 * resetting the task manager, and managing tasks in a map and waiting queue.
 */
class TaskManagerImpl(private val logger: Logger) : TaskManager {
    // Atomic counter for generating unique task IDs
    private val uniqueTaskId = AtomicInteger(0)

    // Map to store active tasks with their task IDs as keys
    private val taskMap: MutableMap<Int, TaskImpl<*>> = HashMap()

    // Queue to store tasks that are waiting to be executed
    private val waitingQueue: Queue<TaskImpl<*>> = LinkedList()

    /**
     * Generates a unique task ID.
     *
     * @return A unique integer task ID.
     */
    override fun generateTaskId(): Int {
        return uniqueTaskId.incrementAndGet()
    }

    /**
     * Handles a received packet by finding the corresponding task and passing the packet to it.
     *
     * @param packet The packet received from the server.
     */
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
     *
     * @param e The exception that caused the reset.
     */
    override fun reset(e: EasyException) {
        // 创建一个临时列表来避免在遍历过程中修改集合
        val tasksToKeep: MutableMap<Int, TaskImpl<*>> = HashMap()
        for ((key, task) in taskMap) {
            // 假设 onReset 返回 boolean 值，表示是否应该保留任务
            if (task.onReset(e)) {
                tasksToKeep[key] = task
            }
        }
        taskMap.clear()
        taskMap.putAll(tasksToKeep)

        // 清空等待队列并重新添加需要保留的任务
        waitingQueue.clear()
        waitingQueue.addAll(tasksToKeep.values)
    }

    /**
     * Prepares the task manager for operation by resuming all tasks in the waiting queue.
     */
    override fun ready() {
        var task: TaskImpl<*>?
        while (waitingQueue.poll().also { task = it } != null) {
            task?.onResume()
        }
    }

    /**
     * Adds a task to the waiting queue.
     *
     * @param task The task to be added to the waiting queue.
     */
    override fun addTaskToWaitingQueue(task: TaskImpl<*>) {
        waitingQueue.add(task)
    }

    /**
     * Adds a task to the task manager for active management.
     *
     * @param task The task to be added.
     */
    override fun addTask(task: TaskImpl<*>) {
        taskMap[task.taskId] = task
    }

    /**
     * Removes a task from the task manager.
     *
     * @param task The task to be removed.
     */
    override fun removeTask(task: Task<*>) {
        taskMap.remove(task.taskId)
    }

    /**
     * Cancels a task by removing it from both the task map and the waiting queue.
     * Logs the result of the cancellation.
     *
     * @param task The task to be canceled.
     */
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
