package com.orientsec.easysocket.session

/**
 * 底层字节读取接口。
 * 抽象具体的读取逻辑（如 Ktor Channel 或 Java InputStream），
 * 为上层 [CommonReader] 提供统一的字节读取能力。
 */
interface ByteReader {
    /**
     * 从底层源读取并填满整个 [data] 数组，直到读满或发生异常。
     * 此方法为挂起函数，在数据不足时会挂起等待。
     *
     * @param data 目标字节数组，方法返回时将被完全填满
     */
    suspend fun readFully(data: ByteArray)

    /**
     * 关闭或取消底层读取资源。
     * 调用后不应再调用 [readFully]。
     */
    fun close()
}