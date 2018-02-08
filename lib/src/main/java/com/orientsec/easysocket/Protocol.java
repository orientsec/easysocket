package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.ReadException;
import com.orientsec.easysocket.exception.WriteException;

import javax.net.ssl.SSLContext;

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
     * @param header 包头原始数据
     * @return 包体大小
     */
    int bodySize(byte[] header) throws ReadException;

    /**
     * 解析消息体
     *
     * @param header    包头字节数组
     * @param bodyBytes 包体字节数组
     * @return 消息体
     */
    Message decodeMessage(byte[] header, byte[] bodyBytes) throws ReadException;

    /**
     * 消息数据转换
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
    byte[] pulseData(Message message);

    /**
     * 授权检验过程
     *
     * @param message 授权消息
     * @return 授权结果
     */
    boolean authorize(Message message);

    /**
     * 授权数据包
     *
     * @param message 授权消息
     * @return 授权请求数据包
     */
    byte[] authorizeData(Message message);

    /**
     * 是否需要授权
     *
     * @return 是否需要授权
     */
    boolean needAuthorize();

    SSLContext sslContext() throws Exception;
}
