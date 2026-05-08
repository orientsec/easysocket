package com.orientsec.easysocket.session

/**
 * 底层字节写入接口，抽象具体的写入逻辑（如 Ktor Channel 或 Java OutputStream）。
 */
interface ByteWriter {
    /**
     * 执行实际的挂起写入操作。
     */
    suspend fun write(data: ByteArray)

    /**
     * 关闭或取消底层资源。
     */
    fun close()
}
