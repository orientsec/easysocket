package com.orientsec.easysocket.error;

public class ErrorBuilder {
    private final String suffix;

    public ErrorBuilder(String suffix) {
        this.suffix = suffix;
    }

    public EasyException create(int code, int type, String message, Exception cause) {
        return new EasyException(code, type, message + suffix, cause);
    }

    public EasyException create(int code, int type, String message) {
        return new EasyException(code, type, message + suffix);
    }
}
