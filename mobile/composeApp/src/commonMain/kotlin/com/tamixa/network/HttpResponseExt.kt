package com.tamixa.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

/**
 * Decodes the JSON body only for 2xx responses. For 401/403/etc. the server often returns
 * [ApiErrorResponse]-shaped JSON; calling [body] with a success DTO causes JsonConvertException.
 */
suspend inline fun <reified T> HttpResponse.bodyIfSuccess(): T? =
    if (status.value in 200..299) body() else null

/**
 * Same as [bodyIfSuccess], but throws a Ktor response exception when status is not 2xx or body is absent.
 */
suspend inline fun <reified T> HttpResponse.requireBodyOrThrow(): T {
    bodyIfSuccess<T>()?.let { return it }
    val text = runCatching { bodyAsText() }.getOrElse { "" }
    if (status.value >= 500) throw ServerResponseException(this, text)
    throw ClientRequestException(this, text)
}
