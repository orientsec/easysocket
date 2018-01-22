package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.exception.ReadException;

import java.io.IOException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2017/12/27 16:10
 * Author: Fredric
 * coding is art not science
 */
public interface Reader {
    void read() throws IOException, ReadException;
}
