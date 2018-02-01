package com.orientsec.easysocket;

import java.io.Serializable;

/**
 * 连接信息服务类
 * Created by xuhao on 2017/5/16.
 */
public final class ConnectionInfo implements Serializable, Cloneable {
    /**
     * IPV4地址
     */
    private String host;
    /**
     * 连接服务器端口号
     */
    private int port;

    public ConnectionInfo(String host, int port) {
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
    public ConnectionInfo clone() {
        return new ConnectionInfo(host, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectionInfo)) {
            return false;
        }

        ConnectionInfo connectInfo = (ConnectionInfo) o;

        if (port != connectInfo.port) {
            return false;
        }
        return host.equals(connectInfo.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }
}
