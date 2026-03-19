package com.tamixa.util

/**
 * Platform-specific log sink. Android uses Log (logcat); iOS uses NSLog (Xcode console).
 * Called by [TamixaLog]. Never log secrets or PII in message/throwable.
 */
internal expect fun platformLog(level: String, tag: String, message: String, throwable: Throwable?)
