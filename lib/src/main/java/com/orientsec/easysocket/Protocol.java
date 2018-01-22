package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.ReadException;
import com.orientsec.easysocket.exception.WriteException;

import java.nio.ByteOrder;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 10:05
 * Author: Fredric
 * coding is art not science
 * <p>
 * 数据协议
 */

public interface Protocol {
    /**
     * 获得包头长度
     *
     * @return 包头的长度
     */
    int headSize();

    /**
     * @param header    包头原始数据
     * @param byteOrder 字节序类型
     * @return 消息体
     */
    Message decodeHead(byte[] header, ByteOrder byteOrder) throws ReadException;

    /**
     * 包头数据转换
     *
     * @param message 消息体
     * @return 消息头的二进制数组
     */
    byte[] encodeHead(Message message) throws WriteException;

    /**
     * 解析消息体
     *
     * @param message   包体
     * @param bodyBytes 包体字节数组
     * @return 消息体
     */
    Message decodeMessage(Message message, byte[] bodyBytes) throws ReadException;

    /**
     * 消息体数据转换
     *
     * @param message 消息体
     * @return 消息体的二进制数组
     */
    byte[] encodeMessage(Message message) throws WriteException;

    /**
     * 心跳消息体
     *
     * @return 心跳消息
     */
    byte[] pulseData();
}
