package com.araro.infrastructure.persistence

import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.domain.CuratedStory
import com.araro.domain.CuratedStoryListing
import com.araro.infrastructure.persistence.CuratedStoryListingProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CuratedStoryRepositoryAdapter(
    private val jpaRepository: CuratedStoryJpaRepository
) : CuratedStoryRepositoryPort {

    override fun findListingByLanguage(language: String, pageable: Pageable): Page<CuratedStoryListing> =
        jpaRepository.findListingByLanguage(language, pageable).map(::toListing)

    override fun findListingByIdIn(ids: List<Long>): List<CuratedStoryListing> =
        if (ids.isEmpty()) emptyList() else jpaRepository.findListingByIdIn(ids).map(::toListing)

    override fun save(story: CuratedStory): CuratedStory {
        val entity = CuratedStoryEntity(
            title = story.title,
            content = story.content,
            theme = story.theme,
            language = story.language,
            age = story.age,
            childName = story.childName,
            wordCount = story.wordCount,
            readingTimeMinutes = story.readingTimeMinutes,
            moral = story.moral,
            audioFileUrl = story.audioFileUrl,
            status = story.status,
            coverImageUrl = story.coverImageUrl,
            coverVideoUrl = story.coverVideoUrl,
            emotionMode = story.emotionMode
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): CuratedStory? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(pageable: Pageable): Page<CuratedStory> =
        jpaRepository.findAll(pageable).map { it.toDomain() }

    override fun findAllWithProcessingFirst(pageable: Pageable): Page<CuratedStory> =
        jpaRepository.findAllWithProcessingFirst(pageable).map { it.toDomain() }

    override fun findByStatusInWithProcessingFirst(statuses: List<String>, pageable: Pageable): Page<CuratedStory> =
        jpaRepository.findByStatusInWithProcessingFirst(statuses, pageable).map { it.toDomain() }

    override fun findByLanguage(language: String, pageable: Pageable): Page<CuratedStory> =
        jpaRepository.findByLanguage(language, pageable).map { it.toDomain() }

    override fun findByStatus(status: String, pageable: Pageable): Page<CuratedStory> =
        jpaRepository.findByStatus(status, pageable).map { it.toDomain() }

    override fun findByStatusIn(statuses: List<String>, pageable: Pageable): Page<CuratedStory> =
        jpaRepository.findByStatusIn(statuses, pageable).map { it.toDomain() }

    override fun existsByTitle(title: String): Boolean =
        jpaRepository.existsByTitleIgnoreCase(title)

    override fun updateAudioUrl(id: Long, audioFileUrl: String) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Curated story not found: $id") }
        entity.audioFileUrl = audioFileUrl
        jpaRepository.save(entity)
    }

    override fun updateStatus(id: Long, status: String) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Curated story not found: $id") }
        entity.status = status
        jpaRepository.save(entity)
    }

    override fun updateStatusBulk(ids: List<Long>, status: String): Int {
        if (ids.isEmpty()) return 0
        val entities = jpaRepository.findAllById(ids)
        entities.forEach { it.status = status }
        jpaRepository.saveAll(entities)
        return entities.size
    }

    override fun updateThemeBulk(ids: List<Long>, theme: String): Int {
        if (ids.isEmpty()) return 0
        val entities = jpaRepository.findAllById(ids)
        entities.forEach { it.theme = theme }
        jpaRepository.saveAll(entities)
        return entities.size
    }

    override fun updateContent(id: Long, content: String, wordCount: Int, readingTimeMinutes: Double) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Curated story not found: $id") }
        entity.content = content
        entity.wordCount = wordCount
        entity.readingTimeMinutes = readingTimeMinutes
        jpaRepository.save(entity)
    }

    override fun clearAudioUrl(id: Long) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Curated story not found: $id") }
        entity.audioFileUrl = null
        jpaRepository.save(entity)
    }

    override fun clearAllAudioUrls(): Int = jpaRepository.clearAllAudioUrls()

    override fun searchByThemeOrTitle(query: String, language: String, pageable: Pageable): Page<CuratedStory> =
        jpaRepository.searchByThemeOrTitle(query.trim().take(100), language, pageable).map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun update(story: CuratedStory): CuratedStory {
        val existing = jpaRepository.findById(story.id).orElseThrow { IllegalArgumentException("Curated story not found: ${story.id}") }
        val entity = CuratedStoryEntity(
            id = story.id,
            title = story.title,
            content = story.content,
            theme = story.theme,
            language = story.language,
            age = story.age,
            childName = story.childName,
            wordCount = story.wordCount,
            readingTimeMinutes = story.readingTimeMinutes,
            moral = story.moral,
            audioFileUrl = story.audioFileUrl,
            status = story.status,
            coverImageUrl = story.coverImageUrl,
            coverVideoUrl = story.coverVideoUrl,
            emotionMode = story.emotionMode,
            createdAt = existing.createdAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }
}

private fun toListing(p: CuratedStoryListingProjection) = CuratedStoryListing(
    id = p.getId(),
    title = p.getTitle(),
    theme = p.getTheme(),
    language = p.getLanguage(),
    age = p.getAge(),
    childName = p.getChildName(),
    wordCount = p.getWordCount(),
    readingTimeMinutes = p.getReadingTimeMinutes(),
    audioFileUrl = p.getAudioFileUrl(),
    status = p.getStatus(),
    coverImageUrl = p.getCoverImageUrl(),
    coverVideoUrl = p.getCoverVideoUrl(),
    createdAt = p.getCreatedAt()
)

private fun CuratedStoryEntity.toDomain() = CuratedStory(
    id = id,
    title = title,
    content = content,
    theme = theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    moral = moral,
    audioFileUrl = audioFileUrl,
    status = status,
    coverImageUrl = coverImageUrl,
    coverVideoUrl = coverVideoUrl,
    createdAt = createdAt,
    emotionMode = emotionMode
)
