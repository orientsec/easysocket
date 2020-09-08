package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.error.EasyException;

public interface Session {

    void open();

    void close(EasyException e);

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
