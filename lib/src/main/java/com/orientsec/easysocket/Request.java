package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.EasyException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/28 13:35
 * Author: Fredric
 * coding is art not science
 * <p>
 * Request send to server
 *
 * @param <IN>  IN param type
 * @param <OUT> OUT param type
 */
public abstract class Request<T, IN, OUT> {
    /**
     * this IN is only send, there is no OUT from server
     */
    private boolean sendOnly;
    protected OUT out;
    protected IN in;

    public boolean isSendOnly() {
        return sendOnly;
    }

    public IN getIn() {
        return in;
    }

    public OUT getOut() {
        return out;
    }

    protected Request() {
    }

    public Request(OUT out) {
        this.out = out;
    }

    public Request(OUT out, boolean sendOnly) {
        this.sendOnly = sendOnly;
        this.out = out;
    }

    public Request(OUT out, IN in) {
        this.out = out;
        this.in = in;
    }

    /**
     * handle IN data here.
     * 对入参进行处理，可以进行统一的业务数据填充、校验，数据编码等
     *
     * @return 处理后的发送数据 类型和{@link Packet#getBody()}一致
     * @throws EasyException 数据处理中的异常
     */
    public abstract byte[] encode(int sequenceId) throws EasyException;

    /**
     * 获取的服务器消息经过{@link HeadParser#decodePacket(HeadParser.Head, byte[])}
     * 处理后，得到{@link Packet}。 在这里将{@link Packet#getBody()}转换为返回结果。
     * 并且可以进行统一的异常封装及其他的一些业务处理。
     *
     * @param data 消息数据
     * @return 返回结果
     * @throws EasyException 异常
     */
    public abstract OUT decode(T data) throws EasyException;
}
