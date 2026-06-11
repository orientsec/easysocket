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
 *
 * @param session 所属的会话实例
 * @param scope 协程作用域
 * @param byteWriter 底层字节写入器
 */
open class CommonQueuedWriter(
    private val session: OperableSession,
    private val scope: CoroutineScope,
    private val byteWriter: ByteWriter
) : Writer {
    /** 等待写入的任务队列 */
    private val writingQueue = ArrayDeque<OperableTask<*>>()

    /** 是否正在执行写入操作 */
    private var isWriting = false

    /**
     * 关闭写入器。
     * 清空写入队列并关闭底层字节写入器。
     */
    override fun shutdown() {
        synchronized(writingQueue) {
            writingQueue.clear()
            isWriting = false
        }
        byteWriter.close()
    }

    /**
     * 提交一个任务到写入队列。
     * 如果当前没有正在写入的任务，立即开始写入；否则加入队列等待。
     *
     * @param task 要提交的写入任务
     */
    override fun submit(task: OperableTask<*>) {
        enqueue(task)
    }

    /**
     * 将任务加入写入队列。
     * 如果当前有任务正在写入，将新任务加入队列末尾；
     * 否则标记为正在写入并立即开始执行。
     *
     * @param task 要入队的任务
     */
    private fun enqueue(task: OperableTask<*>) {
        synchronized(writingQueue) {
            if (isWriting) {
                writingQueue.addLast(task)
                return
            }
            isWriting = true
        }
        task.onSendStart()
        write(task)
    }

    /**
     * 调度下一个写入任务。
     * 从队列中取出下一个任务执行，如果队列为空则标记写入结束。
     */
    private fun scheduleNextWrite() {
        val nextTask = synchronized(writingQueue) {
            if (writingQueue.isNotEmpty()) writingQueue.removeFirst() else null
        }
        if (nextTask == null) {
            synchronized(writingQueue) { isWriting = false }
        } else {
            nextTask.onSendStart()
            write(nextTask)
        }
    }

    /**
     * 执行实际的写入操作。
     * 在协程中将任务数据写入底层字节写入器，
     * 成功后通知任务，失败则关闭会话。
     *
     * @param task 要写入的任务
     */
    private fun write(task: OperableTask<*>) {
        scope.launch {
            try {
                val data = task.data ?: throw Exception("Task data is null")
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
    }

    /**
     * 从写入队列中取消指定任务。
     * 只能取消尚未开始写入的任务。
     *
     * @param task 要取消的任务
     */
    override fun cancel(task: OperableTask<*>) {
        val removed = synchronized(writingQueue) { writingQueue.remove(task) }
        session.logger.i("removed task ${task.taskId} from writing queue: $removed")
    }
}