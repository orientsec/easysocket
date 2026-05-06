package com.orientsec.easysocket.socket

import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.session.OperableSession
import com.orientsec.easysocket.session.Writer
import com.orientsec.easysocket.task.OperableTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.ArrayDeque
import java.util.Deque

/**
 * A writer implementation that manages a queue of tasks.
 * This class ensures sequential writing by scheduling the next task only after
 * the current one completes, mirroring the original Java implementation.
 */
class QueuedWriter(
    private val session: OperableSession,
    private val socket: Socket,
    private val scope: CoroutineScope
) : Writer {
    // 使用 Deque 作为任务队列
    private val writingQueue: Deque<OperableTask<*>> = ArrayDeque()

    // 写入状态标识
    private var isWriting = false

    /**
     * 清空队列并重置状态。
     */
    fun shutdown() {
        writingQueue.clear()
        isWriting = false
    }

    override fun submit(task: OperableTask<*>) {
        enqueue(task)
    }

    /**
     * 入队逻辑：如果正在写入则加入队列，否则立即开始发送。
     */
    private fun enqueue(task: OperableTask<*>) {
        if (isWriting) {
            writingQueue.add(task)
        } else {
            isWriting = true
            task.onSendStart()
            write(task)
        }
    }

    /**
     * 调度下一个任务：从队列头部取出任务并执行。
     */
    private fun scheduleNextWrite() {
        val nextTask = writingQueue.pollFirst()
        if (nextTask == null) {
            isWriting = false
        } else {
            nextTask.onSendStart()
            write(nextTask)
        }
    }

    /**
     * 执行实际的写入操作。
     */
    private fun write(task: OperableTask<*>) {
        scope.launch {
            try {
                // 在 IO 线程池中执行阻塞写入
                withContext(Dispatchers.IO) {
                    val outputStream: OutputStream = socket.getOutputStream()
                    val data = task.data ?: throw IOException("Task data is null")
                    outputStream.write(data)
                    outputStream.flush()
                }
            } catch (e: IOException) {
                // 写入失败：执行失败处理并关闭 Session
                session.logger.w("socket write error ", e)
                val ex = EasyException.Companion.invoke(
                    ErrorCode.WRITE_ERROR, ErrorType.CONNECT,
                    "socket write aborted", session.suffix, e
                )
                task.onSendFailure(ex)
                session.close(ex)
            }
            // 写入成功：执行回调并调度下一个
            task.onSendSuccess()
            scheduleNextWrite()
        }
    }

    override fun cancel(task: OperableTask<*>) {
        val removed = writingQueue.remove(task)
        session.logger.i("removed task ${task.taskId} from writing queue: $removed")
    }
}