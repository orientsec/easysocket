package com.orientsec.easysocket.session.ktor

import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.ByteReader
import com.orientsec.easysocket.session.CommonReader
import com.orientsec.easysocket.session.OperableSession
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readFully

/**
 * Ktor 端的读取实现。
 */
class KtorReader(
    session: OperableSession,
    client: BaseSocketClient,
    socket: Socket
) : CommonReader(
    session,
    client,
    KtorByteReader(socket.openReadChannel())
)

/**
 * Ktor 字节读取的具体实现。
 */
private class KtorByteReader(
    private val readChannel: ByteReadChannel
) : ByteReader {
    override suspend fun readFully(data: ByteArray) {
        readChannel.readFully(data)
    }

    override fun close() {
        readChannel.cancel()
    }
}
