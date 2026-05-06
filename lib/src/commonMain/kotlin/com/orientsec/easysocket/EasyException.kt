package com.orientsec.easysocket

class EasyException(
    val code: Int,
    val type: ErrorType,
    message: String,
    val suffix: String = "",
    cause: Throwable? = null
) : Exception("code: $code, type: $type, message: $message, $suffix", cause)

enum class ErrorType {
    SYSTEM,
    CONNECT,
    TASK
}

object ErrorCode {
    const val UNKNOWN = 1000
    const val SOCKET_CONNECT = 1001
    const val RESPONSE_TIME_OUT = 1002
    const val REQUEST_DATA_EMPTY = 1003
    const val INIT_FAILED = 1004
    const val SESSION_INIT_FAILED = 1005
    const val STOP = 1006
    const val SHUTDOWN = 1007
}
