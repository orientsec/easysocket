package com.orientsec.easysocket

interface Logger {
    fun d(message: String)
    fun i(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}

internal class DefaultLogger(private val tag: String) : Logger {
    override fun d(message: String) = println("DEBUG [$tag]: $message")
    override fun i(message: String) = println("INFO [$tag]: $message")
    override fun w(message: String) = println("WARN [$tag]: $message")
    override fun e(message: String, throwable: Throwable?) {
        println("ERROR [$tag]: $message")
        throwable?.printStackTrace()
    }
}
