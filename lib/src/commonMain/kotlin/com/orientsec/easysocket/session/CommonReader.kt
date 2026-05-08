package com.orientsec.easysocket.session

import com.orientsec.easysocket.HeadParser
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import kotlinx.coroutines.launch

/**
 * 通用的读取逻辑基类。
 */
open class CommonReader(
    private val session: OperableSession,
    private val client: BaseSocketClient,
    protected val byteReader: ByteReader
) : LoopReader(session.logger, client.scope) {

    private val headParser: HeadParser = client.headParser
    private val maxReadSize: Long = client.options.maxReadSizeKb * 1024L

    override suspend fun read() {
        val headLength = headParser.headSize()
        val headBytes = ByteArray(headLength)
        byteReader.readFully(headBytes)
        val head = headParser.parseHead(headBytes)
        val packetSize = head.packetSize
        if (packetSize > maxReadSize) {
            throw Exception("packet size: $packetSize is large than max size: $maxReadSize")
        } else if (packetSize >= 0) {
            val data = ByteArray(packetSize)
            byteReader.readFully(data)
            val packet = headParser.decodePacket(head, data)
            session.handlePacket(packet)
        } else {
            throw Exception("negative packet size: $packetSize")
        }
    }

    override fun beforeLoop() {
        // 子类可按需覆盖
    }

    override fun loopFinish() {
        val e = EasyException(
            ErrorCode.READ_EXIT, ErrorType.CONNECT,
            "socket read aborted", session.suffix, error
        )
        if (isRunning()) {
            client.scope.launch { session.close(e) }
        }
    }

    override fun shutdown() {
        super.shutdown()
        byteReader.close()
    }
}
