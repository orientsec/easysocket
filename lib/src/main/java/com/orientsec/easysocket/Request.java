package com.orientsec.easysocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/28 13:35
 * Author: Fredric
 * coding is art not science
 * <p>
 * Request send to server
 *
 * @param <REQUEST> request param type
 * @param <RESPONSE> response param type
 */
public abstract class Request<T, REQUEST, RESPONSE> {
    /**
     * this request is only send, there is no response from server
     */
    private boolean sendOnly;
    protected REQUEST request;
    protected RESPONSE response;

    public boolean isSendOnly() {
        return sendOnly;
    }

    public REQUEST getRequest() {
        return request;
    }

    public RESPONSE getResponse() {
        return response;
    }

    protected Request() {
    }

    public Request(REQUEST request) {
        this.request = request;
    }


    public Request(REQUEST request, boolean sendOnly) {
        this.sendOnly = sendOnly;
        this.request = request;
    }

    public Request(REQUEST request, RESPONSE response) {
        this.request = request;
        this.response = response;
    }


    /**
     * handle request data here.
     * 对入参进行处理，可以进行统一的业务数据填充、校验，数据编码等
     *
     * @return 处理后的发送数据 类型和{@link Message#getBody()}一致
     * @throws Exception 数据处理中的异常
     */
    public abstract T encode() throws Exception;

    /**
     * 获取的服务器消息经过{@link Protocol#decodeMessage(byte[], byte[])}
     * 处理后，得到{@link Message}。 在这里将{@link Message#getBody()}转换为返回结果。
     * 并且可以进行统一的异常封装及其他的一些业务处理。
     *
     * @param data
     * @return 返回结果
     * @throws Exception 异常
     */
    public abstract RESPONSE decode(T data) throws Exception;
}
