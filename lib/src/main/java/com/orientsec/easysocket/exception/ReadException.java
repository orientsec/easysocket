package com.orientsec.easysocket.exception;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.exception
 * Time: 2018/01/09 13:04
 * Author: Fredric
 * coding is art not science
 * <p>
 * 读异常，发生在{@link com.orientsec.easysocket.Protocol#decodeMessage(byte[], byte[])}过程中
 */
public class ReadException extends EasyException {
    public ReadException() {
    }

    public ReadException(String message) {
        super(message);
    }

    public ReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
