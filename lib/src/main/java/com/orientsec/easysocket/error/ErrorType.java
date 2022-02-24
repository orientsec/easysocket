package com.orientsec.easysocket.error;

public class ErrorType {
    /**
     * 各类系统状态错误：
     * 1.连接被主动断开，如：app进入后台超过设置的休眠时间，或者主动调用shutdown。
     * 2.请求task数量超限，任务无法处理。
     */
    public static final int SYSTEM = 1;
    /**
     * 服务器连接错误。
     * 包括socket连接，数据校验失败，包头解析失败，魔法数不匹配
     * 等各类问题导致连接断开。
     */
    public static final int CONNECT = 2;
    /**
     * 无响应
     */
    public static final int RESPONSE = 3;
}
