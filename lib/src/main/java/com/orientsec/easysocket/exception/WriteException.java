package com.orientsec.easysocket.exception;

import com.orientsec.easysocket.Message;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.exception
 * Time: 2018/01/09 13:47
 * Author: Fredric
 * coding is art not science
 * <p>
 * 写异常，发生在{@link com.orientsec.easysocket.Protocol#encodeMessage(Message)}过程中
 */
public class WriteException extends EasyException {
    public WriteException() {
    }

    public WriteException(String message) {
        super(message);
    }

    public WriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
