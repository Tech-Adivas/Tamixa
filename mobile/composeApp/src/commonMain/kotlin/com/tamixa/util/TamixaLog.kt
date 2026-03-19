package com.tamixa.util

/**
 * Centralized logging for Tamixa mobile app.
 * Android: Log (logcat). iOS: NSLog (Xcode console).
 *
 * Security & compliance:
 * - Never log PII: emails, phones, child names, tokens
 * - Never log passwords, OTP codes, or API keys
 * - Use IDs (storyId, parentId) for traceability
 * - Use [maskUrl], [maskLast4] for URLs/identifiers
 */
object TamixaLog {
    /** Safe description for URLs (no query params/tokens). Use for stream/file URLs. */
    fun maskUrl(url: String?): String = when {
        url == null -> "null"
        url.startsWith("file://") -> "file://"
        url.startsWith("https://") -> "https://..."
        url.startsWith("http://") -> "http://..."
        else -> "url(...)"
    }

    /** Mask identifier to last 4 chars, e.g. "***1234". */
    fun maskLast4(value: String?): String =
        if (value == null || value.length < 4) "***" else "***${value.takeLast(4)}"
    var minimumLevel: Level = Level.DEBUG

    enum class Level(val priority: Int) {
        VERBOSE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        NONE(5)
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (minimumLevel.priority <= Level.VERBOSE.priority) {
            log("V", tag, message, throwable)
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (minimumLevel.priority <= Level.DEBUG.priority) {
            log("D", tag, message, throwable)
        }
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (minimumLevel.priority <= Level.INFO.priority) {
            log("I", tag, message, throwable)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (minimumLevel.priority <= Level.WARN.priority) {
            log("W", tag, message, throwable)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (minimumLevel.priority <= Level.ERROR.priority) {
            log("E", tag, message, throwable)
        }
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        platformLog(level, tag, message, throwable)
        // Do not call printStackTrace() here: on iOS it can cause EXC_BAD_ACCESS when the
        // stack trace string (with '%' in file paths) is used in a format context. iOS
        // platformLog already logs the stack via NSLog("%@", stack).
    }
}
