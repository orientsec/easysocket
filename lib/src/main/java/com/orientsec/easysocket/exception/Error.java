package com.orientsec.easysocket.exception;


public class Error {
    public static class Type {
        //未分类错误
        public static final int DEFAULT = 0;
        //系统错误
        public static final int SYSTEM = 1;
        //网络错误
        public static final int NETWORK = 2;
        //连接错误
        public static final int CONNECT = 3;
        //数据错误
        public static final int DATA = 4;
        //无响应
        public static final int RESPONSE = 5;
    }

    public static class Code {
        //休眠
        public static final int SLEEP = 1;
        //关闭
        public static final int SHUT_DOWN = 2;
        //网络
        public static final int NETWORK_NOT_AVAILABLE = 3;
        //Read IO
        public static final int READ_IO = 4;
        //Write IO
        public static final int WRITE_IO = 5;
        //数据长度
        public static final int STREAM_SIZE = 6;
        //心跳超时
        public static final int PULSE_TIME_OUT = 7;
        //socket连接
        public static final int SOCKET_CONNECT = 8;
        //拒绝执行任务
        public static final int TASK_REFUSED = 9;
        //响应超时
        public static final int RESPONSE_TIME_OUT = 10;
        //Unclassified error in read looper.
        public static final int READ_OTHER = 11;
        //Unclassified error in write looper.
        public static final int WRIT_OTHER = 12;
        //Reader exit.
        public static final int READ_EXIT = 13;
        //Writer exit.
        public static final int WRITE_EXIT = 14;
    }

    public static EasyException create(int code, int type, String message, Throwable t) {
        return new EasyException(code, type, message, t);
    }

    public static EasyException create(int code, int type, String message) {
        return new EasyException(code, type, message);
    }

    public static EasyException create(int code, String message) {
        int type = getType(code);
        return new EasyException(code, type, message);
    }

    public static EasyException create(int code, Throwable t) {
        String message = getMessage(code);
        int type = getType(code);
        return new EasyException(code, type, message, t);
    }

    public static EasyException create(int code) {
        String message = getMessage(code);
        int type = getType(code);
        return new EasyException(code, type, message);
    }

    private static int getType(int code) {
        int type;
        switch (code) {
            case Code.SLEEP:
            case Code.SHUT_DOWN:
                type = Type.SYSTEM;
                break;
            case Code.NETWORK_NOT_AVAILABLE:
                type = Type.NETWORK;
                break;
            case Code.READ_IO:
            case Code.READ_EXIT:
            case Code.WRITE_IO:
            case Code.WRITE_EXIT:
            case Code.SOCKET_CONNECT:
            case Code.PULSE_TIME_OUT:
                type = Type.CONNECT;
                break;
            case Code.STREAM_SIZE:
                type = Type.DATA;
                break;
            case Code.RESPONSE_TIME_OUT:
                type = Type.RESPONSE;
                break;
            case Code.TASK_REFUSED:
            case Code.WRIT_OTHER:
            case Code.READ_OTHER:
            default:
                type = Type.DEFAULT;
                break;
        }
        return type;
    }

    private static String getMessage(int code) {
        String message;
        switch (code) {
            case Code.SLEEP:
                message = "App is sleeping.";
                break;
            case Code.SHUT_DOWN:
                message = "Connection shut down.";
                break;
            case Code.NETWORK_NOT_AVAILABLE:
                message = "Network is not available.";
                break;
            case Code.READ_IO:
                message = "IO error in read looper.";
                break;
            case Code.WRITE_IO:
                message = "IO error in write looper.";
                break;
            case Code.PULSE_TIME_OUT:
                message = "Pulse time out.";
                break;
            case Code.STREAM_SIZE:
                message = "Invalid input stream size.";
                break;
            case Code.SOCKET_CONNECT:
                message = "Fail to start a socket connect.";
                break;
            case Code.TASK_REFUSED:
                message = "Refuse to execute task.";
                break;
            case Code.READ_OTHER:
                message = "Unknown error in read looper.";
                break;
            case Code.WRIT_OTHER:
                message = "Unknown error in write looper.";
                break;
            case Code.READ_EXIT:
                message = "Read looper exit.";
                break;
            case Code.WRITE_EXIT:
                message = "Write looper exit.";
                break;
            case Code.RESPONSE_TIME_OUT:
                message = "Response time out.";
                break;
            default:
                message = "Unknown error.";
                break;
        }
        return message;
    }
}
