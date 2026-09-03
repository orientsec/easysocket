package com.orientsec.easysocket.platform

import com.orientsec.easysocket.session.ByteWriter
import com.orientsec.easysocket.session.CommonQueuedWriter
import com.orientsec.easysocket.session.OperableSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

/**
 * 基于传统 Java Socket 的队列式写入器实现。
 *
 * 继承 [CommonQueuedWriter]，使用 Java Socket 的 OutputStream 作为底层字节写入目标。
 * 由于 OutputStream 是阻塞式 IO，写入操作需要在 IO 调度器上执行。
 *
 * @param session 所属的会话实例
 * @param scope 协程作用域
 * @param socket Java Socket 实例
 */
class QueuedWriter(
    session: OperableSession,
    scope: CoroutineScope,
    socket: Socket
) : CommonQueuedWriter(
    session,
    scope,
    SocketByteWriter(socket)
)

/**
 * 基于 Java Socket OutputStream 的字节写入实现。
 * 使用阻塞式 IO 写入数据，需要在 IO 调度器上执行。
 *
 * @param socket Java Socket 实例
 */
private class SocketByteWriter(
    private val socket: Socket
) : ByteWriter {
    /**
     * 将字节数组写入 Socket 的输出流。
     * 在 IO 调度器上执行阻塞式写入，写入后立即刷新缓冲区。
     *
     * @param data 要写入的字节数组
     */
    override suspend fun write(data: ByteArray) {
        withContext(Dispatchers.IO) {
            val outputStream = socket.getOutputStream()
            outputStream.write(data)
            outputStream.flush()
        }
    }

    /**
     * 关闭写入器。
     * Socket 的关闭由 Session 统一管理，此处不需要额外操作。
     */
    override fun close() {
        // Socket 的关闭由 Session 统一管理，这里不需要额外操作
    }
}