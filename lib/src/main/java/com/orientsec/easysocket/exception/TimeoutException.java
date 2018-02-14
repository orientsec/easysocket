package com.orientsec.easysocket.exception;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.exception
 * Time: 2018/01/10 09:59
 * Author: Fredric
 * coding is art not science
 * <p>
 * 请求超时 请求发送之后，在超时时间内未收到服务器返回
 */
public class TimeoutException extends EasyException {
    public TimeoutException(String message) {
        super(message);
    }
}
