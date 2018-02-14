package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.exception.ReadException;

import java.io.IOException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 16:10
 * Author: Fredric
 * coding is art not science
 */
public interface Reader {
    /**
     * 消息写入
     *
     * @throws IOException   IOException
     * @throws ReadException 读异常，发生在{@link com.orientsec.easysocket.Protocol#decodeMessage(byte[], byte[])}
     *                       过程中
     */
    void read() throws IOException, ReadException;
}
