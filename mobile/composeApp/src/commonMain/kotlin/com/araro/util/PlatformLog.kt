package com.araro.util

/**
 * Platform-specific log sink. Android uses Log (logcat); iOS uses NSLog (Xcode console).
 * Called by [AraroLog]. Never log secrets or PII in message/throwable.
 */
internal expect fun platformLog(level: String, tag: String, message: String, throwable: Throwable?)
