package com.orientsec.easysocket.session

import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.task.OperableTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 通用的顺序写队列管理器。
 *
 * 负责维护写入队列，确保同一时间只有一个任务在执行底层写入操作。
 * 写入操作按提交顺序依次执行，支持任务的提交和取消。
 * 所有操作通过 [scope] 调度到单线程调度器上执行，天然串行化。
 *
 * @param session 所属的会话实例
 * @param scope 协程作用域，使用单线程调度器确保操作串行执行
 * @param byteWriter 底层字节写入器
 */
open class CommonQueuedWriter(
    private val session: OperableSession,
    private val scope: CoroutineScope,
    private val byteWriter: ByteWriter
) : Writer {
    /** 等待写入的任务队列 */
    private val writingQueue = ArrayDeque<OperableTask<*>>()

    /** 是否已关闭 */
    private var isShutdown = false

    /**
     * 关闭写入器。
     * 清空写入队列并关闭底层字节写入器。
     */
    override fun shutdown() {
        isShutdown = true
        writingQueue.clear()
        byteWriter.close()
    }

    /**
     * 提交一个任务到写入队列。
     * 如果当前没有正在写入的任务，立即开始写入；否则加入队列等待。
     *
     * @param task 要提交的写入任务
     */
    override fun submit(task: OperableTask<*>) {
        if (isShutdown) return
        if (writingQueue.isNotEmpty()) {
            writingQueue.addLast(task)
        } else {
            scope.launch {
                performWrite(task)
            }
        }
    }

    /**
     * 执行实际的写入操作，并在完成后调度下一个任务。
     *
     * @param task 要写入的任务
     */
    private suspend fun performWrite(task: OperableTask<*>) {
        if (isShutdown) return
        val data = task.data ?: throw IllegalStateException("Task data is null")
        task.onSendStart()
        try {
            byteWriter.write(data)
            task.onSendSuccess()
        } catch (e: Exception) {
            session.logger.w("write error ", e)
            val ex = e as? EasyException ?: EasyException(
                ErrorCode.WRITE_ERROR, ErrorType.CONNECT,
                "write aborted", session.suffix, e
            )
            task.onSendFailure(ex)
            session.close(ex)
        }
        scheduleNextWrite()
    }

    /**
     * 调度下一个写入任务。
     * 从队列中取出下一个任务执行，如果队列为空则结束写入循环。
     */
    private suspend fun scheduleNextWrite() {
        val nextTask = if (writingQueue.isNotEmpty()) writingQueue.removeFirst() else null
        if (nextTask != null) {
            performWrite(nextTask)
        }
    }

    /**
     * 从写入队列中取消指定任务。
     * 只能取消尚未开始写入的任务。
     *
     * @param task 要取消的任务
     */
    override fun cancel(task: OperableTask<*>) {
        val removed = writingQueue.remove(task)
        session.logger.i("removed task ${task.taskId} from writing queue: $removed")
    }
}