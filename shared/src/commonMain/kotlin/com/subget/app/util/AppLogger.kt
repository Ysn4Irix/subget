package com.subget.app.util

object AppLogger {
    fun d(tag: String, message: String) {
        println("[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        System.err.println("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        println("[$tag] WARN: $message")
        throwable?.printStackTrace()
    }
}
