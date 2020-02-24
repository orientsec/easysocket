package com.orientsec.easysocket;

import java.net.Socket;
import java.net.SocketException;

public interface SocketFactory {
    Socket createSocket() throws SocketException;
}
