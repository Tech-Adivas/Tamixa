package com.tamixa.infrastructure.redis

import com.tamixa.application.port.StoryCachePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("!test")
class StoryCacheAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${app.story.cache-ttl-hours:24}") private val cacheTtlHours: Long
) : StoryCachePort {

    private val keyPrefix = "story:"

    override fun get(cacheKey: String): String? {
        return redisTemplate.opsForValue().get(keyPrefix + cacheKey)
    }

    override fun set(cacheKey: String, content: String) {
        redisTemplate.opsForValue().set(
            keyPrefix + cacheKey,
            content,
            cacheTtlHours,
            TimeUnit.HOURS
        )
    }
}
