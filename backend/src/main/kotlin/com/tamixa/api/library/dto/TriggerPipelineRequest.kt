package com.tamixa.api.library.dto

/**
 * Request DTO for triggering translation pipeline.
 * Currently empty but reserved for future parameters (e.g., specific languages, options).
 */
data class TriggerPipelineRequest(
    val forceRetranslate: Boolean = false
)
