package com.orientsec.easysocket.utils

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object Platform {
    actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun log(level: Int, tag: String, msg: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, msg, throwable)
            LogLevel.INFO -> Log.i(tag, msg, throwable)
            LogLevel.WARN -> Log.w(tag, msg, throwable)
            LogLevel.ERROR -> Log.e(tag, msg, throwable)
        }
    }

    actual object LogLevel {
        actual const val DEBUG = Log.DEBUG
        actual const val INFO = Log.INFO
        actual const val WARN = Log.WARN
        actual const val ERROR = Log.ERROR
    }
}
