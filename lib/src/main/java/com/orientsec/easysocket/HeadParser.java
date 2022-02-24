package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.error.EasyException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 10:05
 * Author: Fredric
 * coding is art not science
 * <p>
 * 数据协议
 */

public interface HeadParser {
    class Head {
        protected int packetSize;

        public Head(int packetSize) {
            this.packetSize = packetSize;
        }

        public int getPacketSize() {
            return packetSize;
        }
    }

    /**
     * 获得包头长度
     *
     * @return 包头的长度
     */
    int headSize();

    /**
     * @param bytes 包头原始数据
     * @return 包体大小
     */
    @NonNull
    Head parseHead(@NonNull byte[] bytes) throws Exception;

    /**
     * 解析消息体
     *
     * @param head      包头
     * @param bodyBytes 包体字节数组
     * @return 消息体
     */
    @NonNull
    Packet decodePacket(@NonNull Head head, @NonNull byte[] bodyBytes)
            throws EasyException;

}
