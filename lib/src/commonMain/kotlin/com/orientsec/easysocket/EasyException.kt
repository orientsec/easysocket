package com.orientsec.easysocket

/**
 * EasySocket 库的异常类，包含错误码和错误类型信息。
 * 仅在库内部使用，外部应使用 [com.orientsec.easysocket.error.EasyException]。
 *
 * @property code 错误码
 * @property type 错误类型
 * @property suffix 附加上下文信息，用于日志标识
 */
class EasyException(
    val code: Int,
    val type: ErrorType,
    message: String,
    val suffix: String = "",
    cause: Throwable? = null
) : Exception("code: $code, type: $type, message: $message, $suffix", cause)

/**
 * 错误类型枚举，仅在库内部使用。
 * 外部应使用 [com.orientsec.easysocket.error.ErrorType]。
 */
enum class ErrorType {
    /** 系统级错误，如客户端主动断开、关闭等 */
    SYSTEM,
    /** 连接级错误，如 Socket 连接失败、数据校验失败等 */
    CONNECT,
    /** 任务级错误，如请求超时、响应未收到等 */
    TASK
}

/**
 * 错误码常量，仅在库内部使用。
 * 外部应使用 [com.orientsec.easysocket.error.ErrorCode]。
 */
object ErrorCode {
    /** 未知错误 */
    const val UNKNOWN = 1000
    /** Socket 连接错误 */
    const val SOCKET_CONNECT = 1001
    /** 响应超时 */
    const val RESPONSE_TIME_OUT = 1002
    /** 请求数据为空 */
    const val REQUEST_DATA_EMPTY = 1003
    /** 客户端初始化失败 */
    const val INIT_FAILED = 1004
    /** 会话初始化失败 */
    const val SESSION_INIT_FAILED = 1005
    /** 客户端已停止 */
    const val STOP = 1006
    /** 客户端已关闭 */
    const val SHUTDOWN = 1007
}