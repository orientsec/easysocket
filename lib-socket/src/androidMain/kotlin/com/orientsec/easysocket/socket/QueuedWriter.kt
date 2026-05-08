package com.orientsec.easysocket.socket

import android.system.Os.socket
import com.orientsec.easysocket.session.ByteWriter
import com.orientsec.easysocket.session.CommonQueuedWriter
import com.orientsec.easysocket.session.OperableSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

/**
 * 传统 Socket 端的写入实现。
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
 * 传统 Socket 字节写入的具体实现类。
 */
private class SocketByteWriter(
    private val socket: Socket
) : ByteWriter {
    override suspend fun write(data: ByteArray) {
        withContext(Dispatchers.IO) {
            val outputStream = socket.getOutputStream()
            outputStream.write(data)
            outputStream.flush()
        }
    }

    override fun close() {
        // Socket 的关闭由 Session 统一管理，这里不需要额外操作
    }
}
