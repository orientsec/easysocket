package com.orientsec.easysocket.session.ktor

import com.orientsec.easysocket.session.ByteWriter
import com.orientsec.easysocket.session.CommonQueuedWriter
import com.orientsec.easysocket.session.OperableSession
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于 Ktor 的写入器实现。
 *
 * 继承 [CommonQueuedWriter]，使用 Ktor 的 [ByteWriteChannel] 作为底层字节写入目标。
 * 通过 Socket 的 openWriteChannel 获取写入通道，实现顺序写入队列管理。
 *
 * @param session 所属的会话实例
 * @param scope 协程作用域
 * @param socket Ktor Socket 实例
 */
class KtorWriter(
    session: OperableSession,
    scope: CoroutineScope,
    socket: Socket
) : CommonQueuedWriter(
    session,
    scope,
    KtorByteWriter(socket.openWriteChannel(autoFlush = true))
)

/**
 * Ktor 字节写入的具体实现类。
 * 封装 Ktor 的 [ByteWriteChannel]，提供 [ByteWriter] 接口的实现。
 *
 * @param writeChannel Ktor 的字节写入通道
 */
private class KtorByteWriter(
    private val writeChannel: ByteWriteChannel
) : ByteWriter {
    /**
     * 将字节数组写入通道。
     * 使用 Ktor 的 writeFully 函数，autoFlush 模式下数据会立即发送。
     *
     * @param data 要写入的字节数组
     */
    override suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        writeChannel.writeFully(data)
    }

    /**
     * 关闭写入通道。
     * 发送 IOException 标记通道已关闭。
     */
    override fun close() {
        writeChannel.cancel(kotlinx.io.IOException("Writer is shut down"))
    }
}