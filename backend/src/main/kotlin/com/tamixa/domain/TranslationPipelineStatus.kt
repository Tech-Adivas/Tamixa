package com.tamixa.domain

/**
 * State machine for story translation pipeline.
 * Transitions must be atomic. Max 3 retries with exponential backoff (2s, 5s, 10s).
 */
enum class TranslationPipelineStatus {
    PENDING,
    TRANSLATING,
    TRANSLATION_FAILED,
    REWRITING,
    REWRITE_FAILED,
    /** Conversational script saved; TTS runs after content approval when using audio-after-approval flow. */
    AWAITING_AUDIO,
    TTS_PROCESSING,
    TTS_FAILED,
    COMPLETED;

    fun isFailed(): Boolean = this == TRANSLATION_FAILED || this == REWRITE_FAILED || this == TTS_FAILED

    fun isTerminal(): Boolean = this == COMPLETED || isFailed()

    /** Includes in-progress and failed states so stuck or failed translations get retried by job/admin. */
    fun canRetry(): Boolean =
        this == REWRITING || this == TRANSLATING || this == TTS_PROCESSING || this == AWAITING_AUDIO || isFailed()
}
