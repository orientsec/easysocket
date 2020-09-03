package com.orientsec.easysocket.error;

public class Errors {
    public static EasyException error(int code, String type, String msg) {
        return new EasyException(code, type, msg);
    }

    public static EasyException systemError(int code, String msg) {
        return new EasyException(code, ErrorType.SYSTEM, msg);
    }

    public static EasyException connectError(int code, String msg) {
        return new EasyException(code, ErrorType.CONNECT, msg);
    }

    public static EasyException shutdown() {
        return systemError(ErrorCode.SHUT_DOWN, "Connection shut down.");
    }
}
