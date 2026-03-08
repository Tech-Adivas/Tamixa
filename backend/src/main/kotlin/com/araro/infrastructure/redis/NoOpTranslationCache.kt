package com.araro.infrastructure.redis

import com.araro.application.port.CachedTranslation
import com.araro.application.port.TranslationCachePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpTranslationCache : TranslationCachePort {

    override fun get(masterStoryId: Long, language: String): CachedTranslation? = null
    override fun set(masterStoryId: Long, language: String, content: CachedTranslation) {}
    override fun hasTranslation(masterStoryId: Long, language: String): Boolean = false
}
