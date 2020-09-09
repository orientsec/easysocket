package com.orientsec.easysocket.client;

import java.io.IOException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 16:10
 * Author: Fredric
 * coding is art not science
 */
public interface Writer {
    /**
     * 消息写入
     *
     * @throws IOException IOException
     */
    void write() throws IOException;
}
