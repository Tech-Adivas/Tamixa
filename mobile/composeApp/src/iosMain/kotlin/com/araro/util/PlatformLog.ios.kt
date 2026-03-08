package com.araro.util

/**
 * iOS log sink. Uses println so logs appear in Xcode console when running from Xcode
 * (Device and Simulator). Avoids NSLog variadic ABI issues on Kotlin/Native (EXC_BAD_ACCESS).
 * Never log secrets, tokens, or PII; use AraroLog.maskUrl / AraroLog.maskLast4 for identifiers.
 */
internal actual fun platformLog(level: String, tag: String, message: String, throwable: Throwable?) {
    val line = "Araro[$level]/$tag: $message"
    println(line)
    throwable?.let {
        val err = "${it::class.simpleName ?: "Throwable"}: ${it.message ?: ""}"
        println("Araro[$level]/$tag: $err")
        val stack = it.stackTraceToString()
        if (stack.length > 2000) {
            stack.chunked(2000).forEach { chunk -> println(chunk) }
        } else {
            println(stack)
        }
    }
}
