package com.orientsec.easysocket

/**
 * 日志接口，定义了日志输出的基本方法。
 * 仅在库内部使用，外部应使用 [com.orientsec.easysocket.utils.Logger]。
 */
interface Logger {
    /** 输出 DEBUG 级别日志 */
    fun d(message: String)
    /** 输出 INFO 级别日志 */
    fun i(message: String)
    /** 输出 WARN 级别日志 */
    fun w(message: String)
    /** 输出 ERROR 级别日志，可附带异常信息 */
    fun e(message: String, throwable: Throwable? = null)
}

/**
 * 默认日志实现，使用标准输出打印日志信息。
 * 仅在库内部使用，作为未提供平台日志实现时的回退方案。
 *
 * @param tag 日志标签，用于标识日志来源
 */
internal class DefaultLogger(private val tag: String) : Logger {
    override fun d(message: String) = println("DEBUG [$tag]: $message")
    override fun i(message: String) = println("INFO [$tag]: $message")
    override fun w(message: String) = println("WARN [$tag]: $message")
    override fun e(message: String, throwable: Throwable?) {
        println("ERROR [$tag]: $message")
        throwable?.printStackTrace()
    }
}