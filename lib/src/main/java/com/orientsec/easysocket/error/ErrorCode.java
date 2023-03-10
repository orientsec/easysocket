package com.orientsec.easysocket.error;

public class ErrorCode {
    //======>客户端错误<======
    //停止
    public static final int STOP = 1;
    //关闭
    public static final int SHUTDOWN = 2;
    //Client初始化失败
    public static final int INIT_FAILED = 3;

    //======>请求任务错误<======
    //拒绝执行任务
    public static final int TASK_REFUSED = 101;
    //响应超时
    public static final int RESPONSE_TIME_OUT = 102;

    //======>连接错误<======
    //初始化失败
    public static final int SESSION_INIT_FAILED = 201;
    //心跳超时
    public static final int PULSE_TIME_OUT = 202;
    //socket连接
    public static final int SOCKET_CONNECT = 203;
    //Reader exit
    public static final int READ_EXIT = 204;
    //Writer exit
    public static final int WRITE_EXIT = 205;
}
