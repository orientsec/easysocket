package com.orientsec.easysocket.exception;

public class ErrorCode {
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
    //Unclassified error in read looper
    public static final int READ_OTHER = 11;
    //Unclassified error in write looper
    public static final int WRIT_OTHER = 12;
    //Reader exit
    public static final int READ_EXIT = 13;
    //Writer exit
    public static final int WRITE_EXIT = 14;
}
