package com.tamixa.ui

import com.tamixa.ui.strings.Strings
import io.ktor.client.plugins.ResponseException

/**
 * Returns a generic error message for UI (inline, dialogs). Does not expose technical
 * causes to users. Only 401 uses a distinct message (session expired) for navigation flow.
 */
fun errorMessageForUser(throwable: Throwable): String {
    val cause = throwable.cause ?: throwable
    return when {
        cause is ResponseException && cause.response.status.value == 401 ->
            Strings.sessionExpired()
        else ->
            Strings.somethingWentWrong()
    }
}

/** True if this is a network/connection/server error (for logging or internal use only). */
fun isApiOrNetworkError(throwable: Throwable): Boolean {
    val cause = throwable.cause ?: throwable
    return cause is ResponseException || isNetworkError(cause) || isConnectionError(cause)
}

private fun isNetworkError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return name == "UnknownHostException" || "UnknownHost" in name ||
        "Could not resolve host" in (t.message ?: "")
}

private fun isConnectionError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return name == "IOException" || "ConnectException" in name || "Socket" in name ||
        "Connection" in (t.message ?: "") || "connection" in (t.message ?: "")
}
