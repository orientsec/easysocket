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
 * 基于 Ktor 的读取器实现。
 *
 * 继承 [CommonReader]，使用 Ktor 的 [ByteReadChannel] 作为底层字节读取源。
 * 通过 Socket 的 openReadChannel 获取读取通道，实现非阻塞的消息读取。
 *
 * @param session 所属的会话实例
 * @param client 所属的客户端实例
 * @param socket Ktor Socket 实例
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
 * 封装 Ktor 的 [ByteReadChannel]，提供 [ByteReader] 接口的实现。
 *
 * @param readChannel Ktor 的字节读取通道
 */
private class KtorByteReader(
    private val readChannel: ByteReadChannel
) : ByteReader {
    /**
     * 从读取通道中填满指定的字节数组。
     * 使用 Ktor 的 readFully 挂起函数，在数据不足时自动挂起等待。
     *
     * @param data 目标字节数组
     */
    override suspend fun readFully(data: ByteArray) {
        readChannel.readFully(data)
    }

    /**
     * 取消读取通道。
     * 调用后读取通道将不再可用。
     */
    override fun close() {
        readChannel.cancel()
    }
}