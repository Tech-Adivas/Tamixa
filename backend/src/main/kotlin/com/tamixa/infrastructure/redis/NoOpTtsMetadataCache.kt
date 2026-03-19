package com.tamixa.infrastructure.redis

import com.tamixa.application.port.TtsMetadataCachePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpTtsMetadataCache : TtsMetadataCachePort {

    override fun getAudioUrl(masterStoryId: Long, language: String): String? = null
    override fun setAudioUrl(masterStoryId: Long, language: String, audioFileUrl: String) {}
    override fun hasAudio(masterStoryId: Long, language: String): Boolean = false
    override fun flushAll() {}
    override fun invalidateForStory(masterStoryId: Long, languages: List<String>) {}
}
