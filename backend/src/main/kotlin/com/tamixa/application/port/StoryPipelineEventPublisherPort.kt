package com.tamixa.application.port

/**
 * Publishes narration pipeline events.
 * Used for on-demand cloned-voice narration (NarrationController).
 * Curated story TTS uses StoryProcessingService directly (approve-narration flow).
 */
interface StoryPipelineEventPublisherPort {

    /**
     * Publish narration request when translation is READY.
     * Triggers conversational narration pipeline (format, validate, SSML, TTS, store).
     * @param toneMode CALM (default) or EXPRESSIVE
     * @param voiceProfiles List of voice identifiers; default ["default"] for built-in voice
     * @param parentId Optional; required for premium voice validation
     */
    fun publishNarrationRequest(translationId: Long, toneMode: com.tamixa.domain.narration.ToneMode? = null, voiceProfiles: List<String>? = null, parentId: Long? = null)
}
