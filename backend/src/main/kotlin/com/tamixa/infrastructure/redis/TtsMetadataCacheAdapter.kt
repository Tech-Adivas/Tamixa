package com.tamixa.infrastructure.redis

import com.tamixa.application.port.TtsMetadataCachePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("!test")
class TtsMetadataCacheAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${app.translation-pipeline.cache-ttl-hours:24}") private val cacheTtlHours: Long
) : TtsMetadataCachePort {

    private val keyPrefix = "tts:"

    override fun getAudioUrl(masterStoryId: Long, language: String): String? =
        redisTemplate.opsForValue().get(key(masterStoryId, language))

    override fun setAudioUrl(masterStoryId: Long, language: String, audioFileUrl: String) {
        redisTemplate.opsForValue().set(key(masterStoryId, language), audioFileUrl, cacheTtlHours, TimeUnit.HOURS)
    }

    override fun hasAudio(masterStoryId: Long, language: String): Boolean =
        redisTemplate.hasKey(key(masterStoryId, language)) == true

    override fun flushAll() {
        redisTemplate.keys("${keyPrefix}*")?.let { keys ->
            if (keys.isNotEmpty()) {
                redisTemplate.delete(keys)
            }
        }
    }

    override fun invalidateForStory(masterStoryId: Long, languages: List<String>) {
        if (languages.isEmpty()) return
        val keys = languages.map { key(masterStoryId, it) }
        redisTemplate.delete(keys)
    }

    private fun key(masterStoryId: Long, language: String) = "${keyPrefix}$masterStoryId:$language"
}
