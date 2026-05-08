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
 * 传统 Socket 端的读取实现。
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
    // 传统 Socket 需要在循环前初始化流
    override fun beforeLoop() {
        (byteReader as SocketByteReader).init()
    }

    private class SocketByteReader(private val socket: Socket) : ByteReader {
        private lateinit var inputStream: InputStream

        fun init() {
            inputStream = socket.getInputStream()
        }

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

        override fun close() {
            // Socket 关闭由 Session 统一处理
        }
    }
}
