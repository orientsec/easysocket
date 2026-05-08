package com.orientsec.easysocket.session

/**
 * 底层字节读取接口。
 */
interface ByteReader {
    /**
     * 从底层源读取并填满整个 [data] 数组，直到读满或发生异常。
     */
    suspend fun readFully(data: ByteArray)

    /**
     * 关闭或取消底层读取资源。
     */
    fun close()
}
