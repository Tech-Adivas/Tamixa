package com.araro.infrastructure.redis

import com.araro.application.port.TtsMetadataCachePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpTtsMetadataCache : TtsMetadataCachePort {

    override fun getAudioUrl(masterStoryId: Long, language: String): String? = null
    override fun setAudioUrl(masterStoryId: Long, language: String, audioFileUrl: String) {}
    override fun hasAudio(masterStoryId: Long, language: String): Boolean = false
}
