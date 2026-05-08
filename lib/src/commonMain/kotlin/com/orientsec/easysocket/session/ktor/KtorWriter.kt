package com.orientsec.easysocket.session.ktor

import com.orientsec.easysocket.session.ByteWriter
import com.orientsec.easysocket.session.CommonQueuedWriter
import com.orientsec.easysocket.session.OperableSession
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope

/**
 * Ktor 端的写入实现。
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
 */
private class KtorByteWriter(
    private val writeChannel: ByteWriteChannel
) : ByteWriter {
    override suspend fun write(data: ByteArray) {
        writeChannel.writeFully(data)
    }

    override fun close() {
        writeChannel.cancel(kotlinx.io.IOException("Writer is shut down"))
    }
}
