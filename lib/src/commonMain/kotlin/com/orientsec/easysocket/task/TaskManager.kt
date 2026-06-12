package com.orientsec.easysocket.task

import com.orientsec.easysocket.PacketHandler
import com.orientsec.easysocket.error.EasyException

/**
 * 任务管理器接口，管理请求任务的生命周期。
 *
 * 继承 [PacketHandler] 用于处理响应包的分发，
 * 提供任务 ID 生成、任务注册/移除/取消、等待队列管理等功能。
 */
interface TaskManager : PacketHandler {

    /**
     * 生成唯一的任务 ID。
     *
     * @return 新的任务 ID
     */
    fun generateTaskId(): Int

    /**
     * 重置任务管理器。
     * 在连接断开或失败时调用，尝试重置所有活跃任务。
     *
     * @param e 导致重置的异常
     */
    fun reset(e: EasyException)

    /**
     * 标记任务管理器为就绪状态。
     * 恢复所有等待队列中的任务。
     * 在连接成功并完成初始化后调用。
     */
    fun ready()

    /**
     * 将任务添加到等待队列。
     * 当连接不可用时调用，任务会在连接可用后被恢复。
     *
     * @param task 要等待的任务
     */
    fun addTaskToWaitingQueue(task: TaskImpl<*>)

    /**
     * 将任务添加到活跃任务映射。
     *
     * @param task 要添加的任务
     */
    fun addTask(task: TaskImpl<*>)

    /**
     * 从活跃任务映射中移除任务。
     *
     * @param task 要移除的任务
     */
    fun removeTask(task: Task<*>)

    /**
     * 取消指定任务。
     * 同时从任务映射和等待队列中移除。
     *
     * @param task 要取消的任务
     */
    fun cancelTask(task: TaskImpl<*>)
}