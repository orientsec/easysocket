package com.orientsec.easysocket.request;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.HeadParser;
import com.orientsec.easysocket.Packet;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/28 13:35
 * Author: Fredric
 * coding is art not science
 * <p>
 * Request send to server
 *
 * @param <R> 返回类型
 */
public abstract class Request<R> implements Encoder, Decoder<R> {
    /**
     * handle output data here.
     * 对请求数据进行处理，可以进行统一的业务数据填充、校验，数据编码等。
     *
     * @return 发送的字节数组
     */
    @Override
    @NonNull
    public abstract Result<byte[]> encode(int sequenceId);

    /**
     * 获取的服务器消息经过{@link HeadParser#decodePacket(HeadParser.Head, byte[])}
     * 处理后，得到{@link Packet}。 在这里将{@link Packet#getBody()}转换为返回结果。
     * 并且可以进行统一的异常封装及其他的一些业务处理。
     *
     * @param data 消息数据
     * @return 接码后的响应结果
     */
    @Override
    @NonNull
    public abstract Result<R> decode(@NonNull Packet data);

}