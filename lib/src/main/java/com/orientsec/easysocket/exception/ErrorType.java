package com.orientsec.easysocket.exception;

public class ErrorType {
    //未分类错误
    public static final String UNCLASSIFIED = "unclassified";
    /**
     * 系统断开连接。
     * app进入后台超过设置的休眠时间，连接会断开，或者主动调用shutdown
     * 断开连接。
     */
    public static final String SYSTEM = "system";
    //无网络连接
    public static final String NETWORK = "network";
    /**
     * 服务器连接错误。
     * 包括socket连接，数据校验失败，包头解析失败，魔法数不匹配
     * 等各类问题导致连接断开。
     */
    public static final String CONNECT = "connect";
    //无响应
    public static final String RESPONSE = "response";
    //任务执行失败
    public static final String TASK = "task";
}
