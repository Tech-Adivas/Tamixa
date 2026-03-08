package com.araro.application.port.narration

import com.araro.domain.narration.StoryNarrationScript

interface StoryNarrationScriptRepositoryPort {

    fun save(script: StoryNarrationScript): StoryNarrationScript

    fun findByTranslationId(translationId: Long): StoryNarrationScript?
}
