package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.DecodeException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/28 13:35
 * Author: Fredric
 * coding is art not science
 */
public abstract class Request<T, R> implements Callback<R> {
    protected boolean sendOnly;
    protected T request;
    protected R response;

    public boolean isSendOnly() {
        return sendOnly;
    }

    public T getRequest() {
        return request;
    }

    public R getResponse() {
        return response;
    }

    protected Request() {
    }

    public Request(T request) {
        this.request = request;
    }

    public Request(T request, boolean sendOnly) {
        this.request = request;
        this.sendOnly = sendOnly;
    }

    public Request(T request, R response) {
        this.request = request;
        this.response = response;
    }

    public abstract Message encode(T request);

    public abstract R decode(Message message) throws DecodeException;
}
