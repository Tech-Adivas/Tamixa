package com.tamixa.infrastructure.redis

import com.tamixa.application.port.CachedTranslation
import com.tamixa.application.port.TranslationCachePort
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

    override fun getByContentHash(sourceHash: String, targetLang: String): CachedTranslation? {
        val json = redisTemplate.opsForValue().get(contentHashKey(sourceHash, targetLang)) ?: return null
        return try {
            objectMapper.readValue(json, CachedTranslation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun setByContentHash(sourceHash: String, targetLang: String, content: CachedTranslation) {
        val json = objectMapper.writeValueAsString(content)
        redisTemplate.opsForValue().set(contentHashKey(sourceHash, targetLang), json, cacheTtlHours, TimeUnit.HOURS)
    }

    private fun key(masterStoryId: Long, language: String) = "${keyPrefix}$masterStoryId:$language"
    private fun contentHashKey(sourceHash: String, targetLang: String) = "${keyPrefix}hash:$sourceHash:$targetLang"
}
