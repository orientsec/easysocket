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


    /**
     * 授权检验过程, 可以通过解析{@param data}获取、校验授权数据
     *
     * @param data 从{@link Packet#getBody()}获取的授权消息体
     * @return 授权结果。如果true，保持连接；如果为false，连接立即断开
     */
    boolean authorize(T data);

    /**
     * 是否需要授权
     *
     * @return 是否需要授权
     */
    boolean needAuthorize();

}
