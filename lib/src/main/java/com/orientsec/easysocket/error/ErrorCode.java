package com.orientsec.easysocket.error;

public class ErrorCode {
    //关闭
    public static final int SHUT_DOWN = 2;
    //心跳超时
    public static final int PULSE_TIME_OUT = 3;
    //socket连接
    public static final int SOCKET_CONNECT = 4;
    //拒绝执行任务
    public static final int TASK_REFUSED = 5;
    //响应超时
    public static final int RESPONSE_TIME_OUT = 6;
    //Reader exit
    public static final int READ_EXIT = 7;
    //Writer exit
    public static final int WRITE_EXIT = 8;
    //初始化失败
    public static final int INIT_FAILED = 10;
}
