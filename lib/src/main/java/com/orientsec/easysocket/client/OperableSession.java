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
     * 连接会话出现错误。
     *
     * @param code 错误码
     * @param type 错误类型
     */
    void onError(int code, int type, String message, Throwable cause);

    Logger getLogger();
}
