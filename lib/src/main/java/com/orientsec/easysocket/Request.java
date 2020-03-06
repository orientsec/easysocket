package com.orientsec.easysocket;

import androidx.annotation.NonNull;

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
public abstract class Request<T, R> {
    /**
     * this IN is only send, there is no OUT from server
     */
    protected boolean sendOnly;
    /**
     * 初始化任务。
     * 在连接可用之前，非初始化请求会进入等待状态，直到连接可用之后，
     * 进行编码、发送。初始化请求在连接成功之后可以直接执行。
     */
    protected boolean init;

    /**
     * 同步请求。和半双工模式的socket类似，请求发送之后等待响应，
     * 同一时间只能有一个同步请求，在一个请求收到响应之后，才可以
     * 发送下一条请求。
     */
    protected boolean sync;

    public boolean isSendOnly() {
        return sendOnly;
    }

    public boolean isInit() {
        return init;
    }

    public boolean isSync() {
        return sync;
    }

    /**
     * handle output data here.
     * 对请求数据进行处理，可以进行统一的业务数据填充、校验，数据编码等。
     *
     * @return 发送的字节数组
     * @throws Exception 数据处理中的异常
     */
    @NonNull
    public abstract byte[] encode(int sequenceId) throws Exception;

    /**
     * 获取的服务器消息经过{@link HeadParser#decodePacket(HeadParser.Head, byte[])}
     * 处理后，得到{@link Packet}。 在这里将{@link Packet#getBody()}转换为返回结果。
     * 并且可以进行统一的异常封装及其他的一些业务处理。
     *
     * @param data 消息数据
     * @return 接码后的响应结果
     * @throws Exception 异常
     */
    @NonNull
    public abstract R decode(@NonNull T data) throws Exception;
}
