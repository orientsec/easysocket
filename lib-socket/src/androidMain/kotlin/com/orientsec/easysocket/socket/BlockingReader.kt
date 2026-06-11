package com.orientsec.easysocket.socket

import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.session.ByteReader
import com.orientsec.easysocket.session.CommonReader
import com.orientsec.easysocket.session.OperableSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.Socket

/**
 * 基于传统 Java Socket 的阻塞式读取器实现。
 *
 * 继承 [CommonReader]，使用 Java Socket 的 [InputStream] 作为底层字节读取源。
 * 由于 InputStream 是阻塞式 IO，读取操作需要在 IO 调度器上执行。
 *
 * @param session 所属的会话实例
 * @param client 所属的客户端实例
 * @param socket Java Socket 实例
 */
class BlockingReader(
    session: OperableSession,
    client: BaseSocketClient,
    socket: Socket
) : CommonReader(
    session,
    client,
    SocketByteReader(socket)
) {
    /**
     * 循环读取前的初始化操作。
     * 传统 Socket 需要在循环开始前获取输入流。
     */
    override fun beforeLoop() {
        (byteReader as SocketByteReader).init()
    }

    /**
     * 基于 Java Socket InputStream 的字节读取实现。
     * 使用阻塞式 IO 读取数据，需要在 IO 调度器上执行。
     *
     * @param socket Java Socket 实例
     */
    private class SocketByteReader(private val socket: Socket) : ByteReader {
        /** Socket 的输入流 */
        private lateinit var inputStream: InputStream

        /**
         * 初始化输入流。
         * 必须在 [readFully] 之前调用。
         */
        fun init() {
            inputStream = socket.getInputStream()
        }

        /**
         * 阻塞式读取，填满指定的字节数组。
         * 循环读取直到数组被填满或遇到流结束。
         *
         * @param data 目标字节数组
         * @throws IOException 如果输入流已关闭或发生 IO 错误
         */
        override suspend fun readFully(data: ByteArray) {
            withContext(Dispatchers.IO) {
                var readCount = 0
                val count = data.size
                while (readCount < count) {
                    val len = inputStream.read(data, readCount, count - readCount)
                    if (len == -1) {
                        throw IOException("input stream closed")
                    }
                    readCount += len
                }
            }
        }

        /**
         * 关闭读取器。
         * Socket 的关闭由 Session 统一管理，此处不需要额外操作。
         */
        override fun close() {
            // Socket 关闭由 Session 统一处理
        }
    }
}