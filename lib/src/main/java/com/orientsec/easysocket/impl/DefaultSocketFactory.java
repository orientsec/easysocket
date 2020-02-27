package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.SocketFactory;

import java.net.Socket;
import java.net.SocketException;


public class DefaultSocketFactory implements SocketFactory {
    @Override
    public Socket createSocket() throws SocketException {
        Socket socket = new Socket();
        //关闭Nagle算法,无论TCP数据报大小,立即发送
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setPerformancePreferences(1, 2, 0);
        return socket;
    }
}
