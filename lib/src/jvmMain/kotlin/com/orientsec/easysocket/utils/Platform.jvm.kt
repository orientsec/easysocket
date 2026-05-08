package com.orientsec.easysocket.utils

import kotlinx.coroutines.CoroutineDispatcher

actual object Platform {
    actual val mainDispatcher: CoroutineDispatcher
        get() = TODO("Not yet implemented")

    actual fun currentTimeMillis(): Long {
        TODO("Not yet implemented")
    }

    actual fun log(
        level: Int,
        tag: String,
        msg: String,
        throwable: Throwable?
    ) {
    }

    actual object LogLevel {
        actual const val DEBUG: Int
            get() = TODO("Not yet implemented")
        actual const val INFO: Int
            get() = TODO("Not yet implemented")
        actual const val WARN: Int
            get() = TODO("Not yet implemented")
        actual const val ERROR: Int
            get() = TODO("Not yet implemented")
    }
}