package com.tamixa.application.port.narration

import com.tamixa.domain.narration.StoryNarrationScript

interface StoryNarrationScriptRepositoryPort {

    fun save(script: StoryNarrationScript): StoryNarrationScript

    fun findByTranslationId(translationId: Long): StoryNarrationScript?

    fun deleteByTranslationId(translationId: Long)
}
