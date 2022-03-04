package com.orientsec.easysocket.error;

public class ErrorBuilder {
    private final String suffix;

    public ErrorBuilder(String suffix) {
        this.suffix = suffix;
    }

    public EasyException create(int code, int type, String message, Exception cause) {
        String msg = message + "  (" + type + "," + code + ")" + suffix;
        return new EasyException(code, type, msg, cause);
    }

    public EasyException create(int code, int type, String message) {
        String msg = message + "  (" + type + "," + code + ")" + suffix;
        return new EasyException(code, type, msg);
    }

}
