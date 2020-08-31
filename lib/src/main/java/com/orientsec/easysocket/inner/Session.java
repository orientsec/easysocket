package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Address;

import java.io.IOException;

public interface Session {
    Address getAddress();

    void open() throws IOException;

    void active();

    void close();
}
