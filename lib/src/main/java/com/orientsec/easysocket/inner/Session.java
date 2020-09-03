package com.orientsec.easysocket.inner;

import java.io.IOException;

public interface Session {

    void open() throws IOException;

    void active();

    void close();
}
