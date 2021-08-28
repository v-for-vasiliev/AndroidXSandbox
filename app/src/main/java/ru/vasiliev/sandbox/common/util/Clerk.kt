package ru.vasiliev.sandbox.common.util

import android.util.Log

class Clerk(private val tag: String) {

    fun d(message: String) = Log.d(
        tag,
        message
    )

    fun w(message: String) = Log.w(
        tag,
        message
    )

    fun e(
        message: String,
        error: Throwable
    ) = Log.e(
        tag,
        message,
        error
    )

    fun e(message: String) = Log.e(
        tag,
        message
    )
}