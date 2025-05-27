package com.orientsec.easysocket.task;

public enum TaskType {
    /**
     * 请求任务。
     */
    REQUEST,
    /**
     * 初始化任务。
     * 在连接可用之前，非初始化请求会进入等待状态，直到连接可用之后，
     * 进行编码、发送。初始化请求在连接成功之后可以直接执行。
     */
    INITIALIZE,
    /**
     * 心跳任务。
     */
    PULSE
}
