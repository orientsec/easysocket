package com.orientsec.easysocket.session

import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import com.orientsec.easysocket.task.OperableTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 通用的顺序写队列管理器。
 * 负责维护写入队列，并确保同一时间只有一个任务在执行底层写入。
 */
open class CommonQueuedWriter(
    private val session: OperableSession,
    private val scope: CoroutineScope,
    private val byteWriter: ByteWriter
) : Writer {
    private val writingQueue = ArrayDeque<OperableTask<*>>()
    private var isWriting = false

    override fun shutdown() {
        synchronized(writingQueue) {
            writingQueue.clear()
            isWriting = false
        }
        byteWriter.close()
    }

    override fun submit(task: OperableTask<*>) {
        enqueue(task)
    }

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

    override fun cancel(task: OperableTask<*>) {
        val removed = synchronized(writingQueue) { writingQueue.remove(task) }
        session.logger.i("removed task ${task.taskId} from writing queue: $removed")
    }
}
