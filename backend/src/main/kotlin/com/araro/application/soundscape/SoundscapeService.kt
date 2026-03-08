package com.araro.application.soundscape

import com.araro.application.port.SoundscapeRepositoryPort
import com.araro.application.port.SoundscapeStoragePort
import com.araro.application.port.SoundscapeUsageRepositoryPort
import com.araro.domain.Soundscape
import com.araro.domain.SoundscapeCategory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SoundscapeService(
    private val soundscapeRepository: SoundscapeRepositoryPort,
    private val soundscapeStorage: SoundscapeStoragePort,
    private val soundscapeUsageRepository: SoundscapeUsageRepositoryPort
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllSoundscapes(): List<Soundscape> {
        return soundscapeRepository.findAll()
    }

    fun getSoundscapesByCategory(category: SoundscapeCategory): List<Soundscape> {
        return soundscapeRepository.getSoundscapesByCategory(category)
    }

    fun getSoundscapesById(soundscapeId: Long): Soundscape? {
        return soundscapeRepository.findById(soundscapeId)
    }

    fun searchSoundscapes(name: String): List<Soundscape> {
        return soundscapeRepository.searchByName(name)
    }

    @Transactional
    fun createSoundscape(
        name: String,
        category: SoundscapeCategory,
        audioBytes: ByteArray,
        description: String? = null,
        durationSeconds: Int = 60
    ): Soundscape {
        val audioUrl = soundscapeStorage.upload(0, audioBytes)
        val soundscape = Soundscape(
            id = 0,
            name = name,
            category = category,
            durationSeconds = durationSeconds,
            audioUrl = audioUrl,
            description = description,
            createdAt = java.time.Instant.now()
        )
        return soundscapeRepository.save(soundscape)
    }

    @Transactional
    fun useSoundscape(parentId: Long, storyId: Long?, soundscapeId: Long) {
        soundscapeUsageRepository.incrementUsage(parentId, storyId, soundscapeId)
        log.info("Soundscape used: parentId={}, storyId={}, soundscapeId={}", parentId, storyId, soundscapeId)
    }

    fun getSoundscapeUsage(parentId: Long): List<com.araro.domain.SoundscapeUsage> {
        return soundscapeUsageRepository.findByParentId(parentId)
    }
}
