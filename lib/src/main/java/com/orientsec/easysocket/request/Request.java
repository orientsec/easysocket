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
     * 初始化请求。
     */
    public static final int INITIALIZE = 1;
    /**
     * 无返回的请求。
     */
    public static final int NO_RESPONSE = 2;
    /**
     * 无任务id。
     */
    public static final int NO_TASK_ID = 4;
    /**
     * 心跳请求。
     */
    public static final int PULSE = 8;

    public final int flag;

    public Request() {
        this.flag = 0;
    }

    public Request(int flag) {
        this.flag = flag;
    }

    /**
     * handle output data here.
     * 对请求数据进行处理，可以进行统一的业务数据填充、校验，数据编码等。
     *
     * @return 发送的字节数组
     * @throws Exception 数据处理中的异常
     */
    @Override
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
    @Override
    @NonNull
    public abstract R decode(@NonNull Packet data) throws Exception;

    /**
     * 请求是否无返回。
     *
     * @return 如果请求无返回，return true。
     */
    public final boolean isNoResponse() {
        return (flag & NO_RESPONSE) == NO_RESPONSE;
    }

    /**
     * 请求是否无需taskId。
     *
     * @return 如果请求无taskId，return true。
     */
    public final boolean isNoTaskId() {
        return (flag & NO_TASK_ID) == NO_TASK_ID;
    }

    /**
     * 初始化任务。
     * 在连接可用之前，非初始化请求会进入等待状态，直到连接可用之后，
     * 进行编码、发送。初始化请求在连接成功之后可以直接执行。
     */
    public final boolean isInitialize() {
        return (flag & INITIALIZE) == INITIALIZE;
    }

    public final boolean isPulse() {
        return (flag & PULSE) == PULSE;
    }
}
