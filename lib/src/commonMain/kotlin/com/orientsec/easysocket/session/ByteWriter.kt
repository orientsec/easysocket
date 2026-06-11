package com.orientsec.easysocket.session

/**
 * 底层字节写入接口。
 * 抽象具体的写入逻辑（如 Ktor Channel 或 Java OutputStream），
 * 为上层 [CommonQueuedWriter] 提供统一的字节写入能力。
 */
interface ByteWriter {
    /**
     * 执行实际的挂起写入操作。
     * 将 [data] 字节数组写入底层输出流。
     *
     * @param data 要写入的字节数组
     */
    suspend fun write(data: ByteArray)

    /**
     * 关闭或取消底层写入资源。
     * 调用后不应再调用 [write]。
     */
    fun close()
}