package com.tamixa.infrastructure.redis

import com.tamixa.application.port.CachedTranslation
import com.tamixa.application.port.TranslationCachePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpTranslationCache : TranslationCachePort {

    override fun get(masterStoryId: Long, language: String): CachedTranslation? = null
    override fun set(masterStoryId: Long, language: String, content: CachedTranslation) {}
    override fun hasTranslation(masterStoryId: Long, language: String): Boolean = false
    override fun getByContentHash(sourceHash: String, targetLang: String): CachedTranslation? = null
    override fun setByContentHash(sourceHash: String, targetLang: String, content: CachedTranslation) {}
}
