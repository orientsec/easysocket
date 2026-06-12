package com.orientsec.easysocket.error

/**
 * EasySocket 库的统一异常类。
 *
 * 包含错误码、错误类型和附加上下文信息，便于问题定位和日志追踪。
 * [suffix] 会自动包含在 [toString] 输出中，方便日志追踪定位。
 *
 * 使用示例：
 * ```kotlin
 * // 无异常原因
 * val e = EasyException(ErrorCode.STOP, ErrorType.SYSTEM, "client stopped", suffix)
 *
 * // 带异常原因
 * val e = EasyException(ErrorCode.STOP, ErrorType.SYSTEM, "client stopped", suffix, cause)
 * ```
 */
open class EasyException : Exception {
    /** 错误码，标识具体的错误类型 */
    val code: Int

    /** 错误类型，标识错误所属的类别（系统/连接/任务） */
    val type: Int

    /** 附加上下文信息，通常包含会话和客户端标识，便于日志追踪 */
    val suffix: String

    /**
     * 构造 EasyException。
     *
     * @param code    错误码
     * @param type    错误类型
     * @param message 详细错误信息
     * @param suffix  附加上下文信息
     */
    constructor(code: Int, type: Int, message: String, suffix: String) : super(message) {
        this.code = code
        this.type = type
        this.suffix = suffix
    }

    /**
     * 构造 EasyException，带异常原因。
     *
     * @param code    错误码
     * @param type    错误类型
     * @param message 详细错误信息
     * @param suffix  附加上下文信息
     * @param cause   导致此异常的原始异常
     */
    constructor(
        code: Int,
        type: Int,
        message: String,
        suffix: String,
        cause: Throwable?
    ) : super(message, cause) {
        this.code = code
        this.type = type
        this.suffix = suffix
    }

    override fun toString(): String {
        return "EasyException: $message ($type, $code) {$suffix}"
    }
}