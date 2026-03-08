package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StoryNarrationScriptJpaRepository : JpaRepository<StoryNarrationScriptEntity, Long> {

    fun findByTranslationId(translationId: Long): StoryNarrationScriptEntity?
}
