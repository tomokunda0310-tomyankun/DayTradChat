// /app/src/main/java/com/daytradchat/papa/util/AppLogger.kt
// ver 1.00-00
package com.daytradchat.papa.util

import android.util.Log

object AppLogger {
    private const val TAG = "DayTradeChat"

    fun d(message: String) = Log.d(TAG, message)
    fun e(message: String, throwable: Throwable? = null) = Log.e(TAG, message, throwable)
}
