package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.EasyException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 10:05
 * Author: Fredric
 * coding is art not science
 * <p>
 * 数据协议
 */

public interface HeadParser<T> {
    interface Head {
        int bodySize();
    }

    /**
     * 获得包头长度
     *
     * @return 包头的长度
     */
    int headSize();

    /**
     * @param header 包头原始数据
     * @return 包体大小
     */
    Head parseHead(byte[] header) throws EasyException;

    /**
     * 解析消息体
     *
     * @param head    包头
     * @param bodyBytes 包体字节数组
     * @return 消息体
     */
    Packet<T> decodePacket(Head head, byte[] bodyBytes) throws EasyException;

}
