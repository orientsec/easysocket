package com.orientsec.easysocket.task

import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.utils.Logger

/**
 * 任务管理器的实现类，负责管理所有活跃的请求任务。
 *
 * 核心职责：
 * 1. 生成唯一的任务 ID
 * 2. 管理活跃任务映射（taskId -> Task）
 * 3. 管理等待队列（连接不可用时的任务）
 * 4. 处理响应包的分发
 * 5. 连接断开时重置任务状态
 * 6. 连接可用时恢复等待中的任务
 *
 * @param logger 日志记录器
 */
class TaskManagerImpl(private val logger: Logger) : TaskManager {
    /** 任务 ID 计数器 */
    private var uniqueTaskId = 0

    /** 活跃任务映射，key 为 taskId */
    private val taskMap: MutableMap<Int, TaskImpl<*>> = mutableMapOf()

    /** 等待队列，存放连接不可用时的任务 */
    private val waitingQueue = ArrayDeque<TaskImpl<*>>()

    /**
     * 生成唯一的任务 ID。
     * 使用自增计数器确保唯一性。
     *
     * @return 新的任务 ID
     */
    @Synchronized
    override fun generateTaskId(): Int {
        return ++uniqueTaskId
    }

    /**
     * 处理接收到的响应包。
     * 根据 taskId 查找对应的任务，将响应包传递给它。
     * 如果找不到对应任务，输出警告日志。
     *
     * @param packet 接收到的响应包
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
     * 重置任务管理器。
     * 在连接断开或失败时调用，尝试重置所有活跃任务。
     * 可以重试的任务会保留在任务映射和等待队列中，
     * 不可重试的任务会被移除。
     *
     * @param e 导致重置的异常
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

        // 清空等待队列，将可重试的任务重新加入
        waitingQueue.clear()
        waitingQueue.addAll(tasksToKeep.values)
    }

    /**
     * 标记任务管理器为就绪状态。
     * 恢复所有等待队列中的任务。
     * 在连接成功并完成初始化后调用。
     */
    @Synchronized
    override fun ready() {
        while (waitingQueue.isNotEmpty()) {
            val task = waitingQueue.removeFirst()
            task.onResume()
        }
    }

    /**
     * 将任务添加到等待队列。
     * 当连接不可用时调用，任务会在连接可用后被恢复。
     *
     * @param task 要等待的任务
     */
    @Synchronized
    override fun addTaskToWaitingQueue(task: TaskImpl<*>) {
        waitingQueue.addLast(task)
    }

    /**
     * 将任务添加到活跃任务映射。
     *
     * @param task 要添加的任务
     */
    @Synchronized
    override fun addTask(task: TaskImpl<*>) {
        taskMap[task.taskId] = task
    }

    /**
     * 从活跃任务映射中移除任务。
     *
     * @param task 要移除的任务
     */
    @Synchronized
    override fun removeTask(task: Task<*>) {
        taskMap.remove(task.taskId)
    }

    /**
     * 取消指定任务。
     * 同时从任务映射和等待队列中移除。
     *
     * @param task 要取消的任务
     */
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