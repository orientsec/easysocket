package com.orientsec.easysocket.client;

import androidx.annotation.MainThread;

import com.orientsec.easysocket.utils.Logger;

public interface OperableSession extends Session {

    /**
     * 打开session。
     */
    @MainThread
    void open();

    /**
     * 关闭session。
     *
     * @param code 错误码
     * @param type 错误类型
     */
    @MainThread
    void close(int code, int type, String message);

    /**
     * 向连接主线程post一个异常。
     *
     * @param code 错误码
     * @param type 错误类型
     */
    void postClose(int code, int type, String message, Exception cause);

    Logger getLogger();
}
