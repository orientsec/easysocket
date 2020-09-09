package com.orientsec.easysocket.client;

/**
 * 连接状态
 * <p>
 * IDLE
 * 空闲状态 IDLE -> STARTING
 * <p>
 * STARTING 启动中
 * 1.STARTING -> IDLE (建连失败)
 * 2.STARTING -> CONNECT (建连成功)
 * <p>
 * CONNECT 连接成功
 * 1.CONNECT -> AVAILABLE (初始化成功)
 * 2.CONNECT -> IDLE (初始化失败)
 * <p>
 * AVAILABLE 连接可用
 * AVAILABLE -> IDLE (连接断开)
 * <p>
 */
enum State {
    IDLE, STARTING, CONNECT, AVAILABLE, DETACH
}
