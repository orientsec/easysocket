package com.orientsec.easysocket.client;

import androidx.annotation.MainThread;

import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.error.EasyException;

public interface Session extends PacketHandler {
    /**
     * 打开session。
     */
    @MainThread
    void open();

    /**
     * 关闭session。
     *
     * @param e 异常。
     */
    @MainThread
    void close(EasyException e);

    /**
     * 向连接主线程post一个异常。
     *
     * @param e 异常。
     */
    void postError(EasyException e);

    /**
     * 是否连接
     *
     * @return 是否连接
     */
    boolean isConnect();

    /**
     * 连接是否可达
     *
     * @return 是否可达
     */
    boolean isAvailable();
}
