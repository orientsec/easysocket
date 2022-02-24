package com.orientsec.easysocket.error;

public class Errors {
    public static EasyException error(int code, int type, String msg) {
        return new EasyException(code, type, msg);
    }

    public static EasyException connectError(int code, String msg) {
        return new EasyException(code, ErrorType.CONNECT, msg);
    }

    public static EasyException connectError(int code, String msg, Throwable cause) {
        return new EasyException(code, ErrorType.CONNECT, msg, cause);
    }

    public static EasyException shutdown() {
        return error(ErrorCode.SHUTDOWN, ErrorType.SYSTEM, "Socket client shut down.");
    }

    public static EasyException stop() {
        return error(ErrorCode.STOP, ErrorType.SYSTEM, "Socket client stop.");
    }
}
