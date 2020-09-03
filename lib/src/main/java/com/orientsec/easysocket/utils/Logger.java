package com.orientsec.easysocket.utils;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2018/01/09 10:04
 * Author: Fredric
 * coding is art not science
 */
public interface Logger {

    void e(String msg);

    void e(String msg, Throwable t);

    void i(String msg);

    void i(String msg, Throwable t);

    void w(String msg);

    void d(String msg);
}
