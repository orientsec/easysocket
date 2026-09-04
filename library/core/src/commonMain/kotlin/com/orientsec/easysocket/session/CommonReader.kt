package com.orientsec.easysocket.session

import com.orientsec.easysocket.HeadParser
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import kotlinx.coroutines.withContext

/**
 * 通用的读取逻辑基类。
 *
 * 封装了基于协议头解析的消息读取流程：
 * 1. 读取固定长度的协议头
 * 2. 解析协议头获取消息体长度
 * 3. 读取消息体
 * 4. 解码为 [com.orientsec.easysocket.Packet] 并分发给会话处理
 *
 * 子类只需提供 [ByteReader] 实现即可完成完整的消息读取功能。
 *
 * @param session 所属的会话实例
 * @param client 所属的客户端实例
 * @param byteReader 底层字节读取器
 */
open class CommonReader(
    private val session: OperableSession,
    private val client: BaseSocketClient,
    protected val byteReader: ByteReader
) : LoopReader(session.logger, client.scope) {

    /** 协议头解析器 */
    private val headParser: HeadParser = client.headParser

    /** 单次读取最大字节数，防止读取超大包导致 OOM */
    private val maxReadSize: Long = client.options.maxReadSizeKb * 1024L

    /**
     * 执行一次完整的消息读取。
     * 流程：读取协议头 -> 解析协议头 -> 读取消息体 -> 解码数据包 -> 分发处理
     */
    override suspend fun read() = withContext(client.options.codecDispatcher) {
        // 1. 读取协议头
        val headLength = headParser.headSize()
        val headBytes = ByteArray(headLength)
        byteReader.readFully(headBytes)

        // 2. 解析协议头，获取消息体长度
        val head = headParser.parseHead(headBytes)
        val packetSize = head.packetSize

        // 3. 校验消息体大小
        if (packetSize > maxReadSize) {
            throw Exception("packet size: $packetSize is large than max size: $maxReadSize")
        } else if (packetSize >= 0) {
            // 4. 读取消息体
            val data = ByteArray(packetSize)
            byteReader.readFully(data)

            // 5. 解码并分发数据包
            val packet = headParser.decodePacket(head, data)
            session.handlePacket(packet)
        } else {
            throw Exception("negative packet size: $packetSize")
        }
    }

    /**
     * 循环读取前的初始化操作。
     * 子类可按需覆盖，如初始化输入流等。
     */
    override fun beforeLoop() {
        // 子类可按需覆盖
    }

    /**
     * 循环读取结束后的处理。
     * 如果读取循环因错误而退出，关闭当前会话。
     */
    override fun loopFinish() {
        if (isRunning()) {
            EasyException(
                ErrorCode.READ_EXIT, ErrorType.CONNECT,
                "socket read aborted", session.suffix, error
            ).let { session.close(it) }
        }
    }

    /**
     * 关闭读取器，同时关闭底层字节读取器。
     */
    override fun shutdown() {
        super.shutdown()
        byteReader.close()
    }
}