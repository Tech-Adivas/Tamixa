package com.araro.infrastructure.redis

import com.araro.application.port.CachedTranslation
import com.araro.application.port.TranslationCachePort
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("!test")
class TranslationCacheAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${app.translation-pipeline.cache-ttl-hours:24}") private val cacheTtlHours: Long
) : TranslationCachePort {

    private val keyPrefix = "translation:"

    override fun get(masterStoryId: Long, language: String): CachedTranslation? {
        val json = redisTemplate.opsForValue().get(key(masterStoryId, language)) ?: return null
        return try {
            objectMapper.readValue(json, CachedTranslation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun set(masterStoryId: Long, language: String, content: CachedTranslation) {
        val json = objectMapper.writeValueAsString(content)
        redisTemplate.opsForValue().set(key(masterStoryId, language), json, cacheTtlHours, TimeUnit.HOURS)
    }

    override fun hasTranslation(masterStoryId: Long, language: String): Boolean =
        redisTemplate.hasKey(key(masterStoryId, language)) == true

    private fun key(masterStoryId: Long, language: String) = "${keyPrefix}$masterStoryId:$language"
}
