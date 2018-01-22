package com.orientsec.easysocket.exception;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.exception
 * Time: 2017/12/26 16:37
 * Author: Fredric
 * coding is art not science
 */

public class EasyException extends Exception {

    public EasyException() {
    }

    public EasyException(String message) {
        super(message);
    }

    public EasyException(String message, Throwable cause) {
        super(message, cause);
    }
}
