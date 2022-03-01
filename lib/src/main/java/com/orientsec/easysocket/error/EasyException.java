package com.orientsec.easysocket.error;

import androidx.annotation.NonNull;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.exception
 * Time: 2017/12/26 16:37
 * Author: Fredric
 * coding is art not science
 */

public class EasyException extends Exception {
    public final int code;
    public final int type;

    public EasyException(int code, int type, String message) {
        super(message);
        this.code = code;
        this.type = type;
    }

    public EasyException(int code, int type, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        return "EasyException{" +
                "code=" + code +
                ", type=" + type +
                ", message=" + getMessage() +
                '}';
    }

}
