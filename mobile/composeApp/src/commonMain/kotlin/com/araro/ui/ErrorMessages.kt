package com.araro.ui

import com.araro.ui.strings.Strings
import io.ktor.client.plugins.ResponseException

/** Maps exceptions to user-friendly, localized snackbar messages. */
fun errorMessageForUser(throwable: Throwable): String {
    val cause = throwable.cause ?: throwable
    return when {
        cause is ResponseException -> when (cause.response.status.value) {
            401 -> Strings.sessionExpired()
            429 -> Strings.rateLimitExceeded()
            403 -> Strings.noPermission()
            404 -> Strings.notFound()
            in 500..599 -> Strings.serverError()
            else -> Strings.somethingWentWrong()
        }
        isNetworkError(cause) -> Strings.noInternet()
        isConnectionError(cause) -> Strings.connectionProblem()
        else -> throwable.message?.takeIf { it.isNotBlank() } ?: Strings.somethingWentWrong()
    }
}

/** Cross-platform: network/DNS errors (UnknownHostException on JVM, similar on Native). */
private fun isNetworkError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return name == "UnknownHostException" || "UnknownHost" in name ||
        "Could not resolve host" in (t.message ?: "")
}

/** Cross-platform: connection/IO errors (IOException on JVM, similar on Native). */
private fun isConnectionError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return name == "IOException" || "ConnectException" in name || "Socket" in name ||
        "Connection" in (t.message ?: "") || "connection" in (t.message ?: "")
}
