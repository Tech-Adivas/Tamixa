package com.araro.application.port

/**
 * Publishes pipeline events for translation and TTS.
 * Exactly-once semantics via event key: "${masterStoryId}:${language}"
 */
interface StoryPipelineEventPublisherPort {

    fun publishTranslationRequest(
        masterStoryId: Long,
        language: String,
        sourceContent: String,
        sourceTitle: String?,
        sourceMoral: String?
    )

    fun publishTtsRequest(masterStoryId: Long, language: String, content: String)

    /**
     * Publish narration request when translation is READY.
     * Triggers conversational narration pipeline (format, validate, SSML, TTS, store).
     * @param toneMode CALM (default) or EXPRESSIVE
     * @param voiceProfiles List of voice identifiers; default ["default"] for built-in voice
     * @param parentId Optional; required for premium voice validation
     */
    fun publishNarrationRequest(translationId: Long, toneMode: com.araro.domain.narration.ToneMode? = null, voiceProfiles: List<String>? = null, parentId: Long? = null)
}
