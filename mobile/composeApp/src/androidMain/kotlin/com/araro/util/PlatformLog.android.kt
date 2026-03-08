package com.araro.util

import android.util.Log

internal actual fun platformLog(level: String, tag: String, message: String, throwable: Throwable?) {
    when (level) {
        "V" -> Log.v(tag, message)
        "D" -> Log.d(tag, message)
        "I" -> Log.i(tag, message)
        "W" -> if (throwable != null) Log.w(tag, throwable) else Log.w(tag, message)
        "E" -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        else -> Log.d(tag, message)
    }
    throwable?.let {
        if (level != "W" && level != "E") Log.w(tag, "${it::class.simpleName}: ${it.message}", it)
    }
}
