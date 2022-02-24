package com.orientsec.easysocket.client;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.error.EasyException;

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
     * @throws EasyException 读异常，发生在{@link HeadParser#decodePacket(HeadParser.Head, byte[])}
     *                       过程中
     */
    void read() throws Exception;
}
