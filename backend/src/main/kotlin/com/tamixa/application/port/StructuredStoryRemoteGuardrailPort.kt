package com.tamixa.application.port

import com.tamixa.application.story.ModerationContext
import com.tamixa.application.story.StructuredStoryPayload

/**
 * Optional HTTP guardrail for structured story JSON, typically implemented by a **separate Python service**
 * (reusable across products). When disabled, no bean is present.
 */
fun interface StructuredStoryRemoteGuardrailPort {
    /**
     * @param maxStoryWordsAllowed consumer-computed cap (e.g. age-based ∩ app max) — service stays project-agnostic.
     */
    fun validateStructuredStory(
        payload: StructuredStoryPayload,
        context: ModerationContext,
        maxStoryWordsAllowed: Int
    )
}
