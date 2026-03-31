package com.tamixa.network

/**
 * Story (or related) content was rejected by server-side moderation (HTTP 422).
 * [message] is the API error text when available; safe to show to the parent.
 */
class ContentModerationFailureException(
    message: String
) : Exception(message)
