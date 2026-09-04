package com.orientsec.easysocket.session

/**
 * 会话的连接状态枚举。
 *
 * 状态流转图：
 * ```
 * IDLE -> STARTING -> CONNECTED -> AVAILABLE -> DETACHED
 *                  \-> DETACHED  \-> DETACHED
 * ```
 *
 * 各状态说明：
 * - [IDLE]: 初始空闲状态，会话刚创建尚未开始连接
 * - [STARTING]: 正在连接中，Socket 连接正在建立
 * - [CONNECTED]: Socket 连接已建立，等待会话初始化（如登录）
 * - [AVAILABLE]: 会话可用，所有请求可以正常发送
 * - [DETACHED]: 已断开，会话不再活跃
 */
enum class State {
    /** 空闲状态，会话尚未启动 */
    IDLE,
    /** 正在连接中 */
    STARTING,
    /** 已连接，等待初始化完成 */
    CONNECTED,
    /** 可用状态，可以发送请求 */
    AVAILABLE,
    /** 已断开，会话结束 */
    DETACHED
}