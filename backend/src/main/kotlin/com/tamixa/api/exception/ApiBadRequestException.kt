package com.tamixa.api.exception

/**
 * Client-facing 400 with a stable [code] for programmatic handling (e.g. unknown generation topic).
 */
class ApiBadRequestException(
    val code: String,
    message: String,
) : RuntimeException(message)

object ApiErrorCodes {
    const val UNKNOWN_GENERATION_TOPIC = "UNKNOWN_GENERATION_TOPIC"
    const val THEME_OR_TOPIC_REQUIRED = "THEME_OR_TOPIC_REQUIRED"
    const val GENERATION_LANGUAGE_NOT_SUPPORTED = "GENERATION_LANGUAGE_NOT_SUPPORTED"
}
