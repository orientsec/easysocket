package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * 连接信息服务类
 * Created by Fredric on 2017/5/16.
 */
public final class Address implements Serializable, Cloneable {
    /**
     * IPV4地址
     */
    private final String host;
    /**
     * 连接服务器端口号
     */
    private final int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 获取传入的IP地址
     *
     * @return ip地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 获取传入的端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address)) {
            return false;
        }

        Address connectInfo = (Address) o;

        return port == connectInfo.port && host.equals(connectInfo.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Address[" +
                "host=" + host +
                ", port=" + port +
                ']';
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
