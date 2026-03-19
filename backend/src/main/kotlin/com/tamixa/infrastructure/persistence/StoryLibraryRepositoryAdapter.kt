package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.LibraryStoryListing
import java.time.Instant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class StoryLibraryRepositoryAdapter(
    private val jpaRepository: LibraryStoryJpaRepository
) : StoryLibraryRepositoryPort {
    private fun touch(entity: LibraryStoryEntity) {
        entity.updatedAt = Instant.now()
    }

    override fun findListingByLanguage(language: String, pageable: Pageable): Page<LibraryStoryListing> =
        jpaRepository.findListingByLanguage(language, pageable).map(::toListing)

    override fun findListingByLanguageAndNarrationApproved(language: String, pageable: Pageable): Page<LibraryStoryListing> =
        jpaRepository.findListingByLanguageAndNarrationApprovedAtNotNull(language, pageable).map(::toListing)

    override fun findListingByLanguageAndNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<LibraryStoryListing> =
        jpaRepository.findListingByLanguageAndNarrationApprovedAndTheme(language, theme, pageable).map(::toListing)

    override fun findDistinctThemesByNarrationApproved(language: String): List<String> =
        jpaRepository.findDistinctThemesByNarrationApproved(language)

    override fun findDistinctThemesByNarrationApprovedAndTranslationLanguage(language: String): List<String> =
        jpaRepository.findDistinctThemesByNarrationApprovedAndTranslationLanguage(language)

    override fun findListingByIdIn(ids: List<Long>): List<LibraryStoryListing> =
        if (ids.isEmpty()) emptyList() else jpaRepository.findListingByIdIn(ids).map(::toListing)

    override fun save(story: LibraryStory): LibraryStory {
        val entity = LibraryStoryEntity(
            title = story.title,
            content = story.content,
            theme = story.theme,
            category = story.category,
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
            updatedAt = story.updatedAt,
            storyOwner = story.storyOwner,
            convertPromptUsed = story.convertPromptUsed,
            emotionMode = story.emotionMode,
            narrationApprovedAt = story.narrationApprovedAt,
            rejectMarkedAt = story.rejectMarkedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): LibraryStory? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findAll(pageable).map { it.toDomain() }

    override fun findAllWithProcessingFirst(pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findAllWithProcessingFirst(pageable).map { it.toDomain() }

    override fun findByStatusInWithProcessingFirst(statuses: List<String>, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByStatusInWithProcessingFirst(statuses, pageable).map { it.toDomain() }

    override fun findByLanguage(language: String, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByLanguage(language, pageable).map { it.toDomain() }

    override fun findByLanguageAndNarrationApproved(language: String, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByLanguageAndNarrationApprovedAtNotNull(language, pageable).map { it.toDomain() }

    override fun findByLanguageAndNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByLanguageAndNarrationApprovedAtNotNullAndTheme(language, theme, pageable).map { it.toDomain() }

    override fun findByStatus(status: String, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByStatus(status, pageable).map { it.toDomain() }

    override fun findByStatusPublishedAndNarrationApprovedAtNull(pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByStatusPublishedAndNarrationApprovedAtNull(pageable).map { it.toDomain() }

    override fun findByNarrationApprovedAtNotNull(pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByNarrationApprovedAtNotNull(pageable).map { it.toDomain() }

    override fun findByStatusIn(statuses: List<String>, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.findByStatusIn(statuses, pageable).map { it.toDomain() }

    override fun existsByTitle(title: String): Boolean =
        jpaRepository.existsByTitleIgnoreCase(title)

    override fun updateAudioUrl(id: Long, audioFileUrl: String) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.audioFileUrl = audioFileUrl
        touch(entity)
        jpaRepository.save(entity)
    }

    override fun updateStatus(id: Long, status: String) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.status = status
        touch(entity)
        jpaRepository.save(entity)
    }

    override fun updateStatusAndReviewNotes(id: Long, status: String, reviewNotes: String?) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.status = status
        entity.reviewNotes = reviewNotes
        touch(entity)
        jpaRepository.save(entity)
    }

    override fun updateStatusBulk(ids: List<Long>, status: String): Int {
        if (ids.isEmpty()) return 0
        val entities = jpaRepository.findAllById(ids)
        entities.forEach {
            it.status = status
            touch(it)
        }
        jpaRepository.saveAll(entities)
        return entities.size
    }

    override fun updateThemeBulk(ids: List<Long>, theme: String): Int {
        if (ids.isEmpty()) return 0
        val entities = jpaRepository.findAllById(ids)
        entities.forEach {
            it.theme = theme
            touch(it)
        }
        jpaRepository.saveAll(entities)
        return entities.size
    }

    override fun updateContent(id: Long, content: String, wordCount: Int, readingTimeMinutes: Double) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.content = content
        entity.wordCount = wordCount
        entity.readingTimeMinutes = readingTimeMinutes
        touch(entity)
        jpaRepository.save(entity)
    }

    override fun clearAudioUrl(id: Long) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.audioFileUrl = null
        touch(entity)
        jpaRepository.save(entity)
    }

    override fun clearAllAudioUrls(): Int = jpaRepository.clearAllAudioUrls()

    override fun searchByThemeOrTitle(query: String, language: String, pageable: Pageable): Page<LibraryStory> =
        jpaRepository.searchByThemeOrTitle(query.trim().take(100), language, pageable).map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun update(story: LibraryStory): LibraryStory {
        val existing = jpaRepository.findById(story.id).orElseThrow { IllegalArgumentException("Library story not found: ${story.id}") }
        val entity = LibraryStoryEntity(
            id = story.id,
            title = story.title,
            content = story.content,
            theme = story.theme,
            category = story.category,
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
            createdAt = existing.createdAt,
            updatedAt = story.updatedAt,
            storyOwner = story.storyOwner,
            convertPromptUsed = story.convertPromptUsed,
            narrationApprovedAt = story.narrationApprovedAt,
            reviewNotes = story.reviewNotes,
            rejectMarkedAt = story.rejectMarkedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun updateRejectMarkedAt(id: Long, markedAt: Instant?) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.rejectMarkedAt = markedAt
        touch(entity)
        jpaRepository.save(entity)
    }

    override fun updateNarrationApprovedAt(id: Long, approvedAt: Instant?) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Library story not found: $id") }
        entity.narrationApprovedAt = approvedAt
        touch(entity)
        jpaRepository.save(entity)
    }
}

private fun toListing(p: LibraryStoryListingProjection) = LibraryStoryListing(
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

private fun LibraryStoryEntity.toDomain() = LibraryStory(
    id = id,
    title = title,
    content = content,
    theme = theme,
    category = category,
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
    updatedAt = updatedAt,
    storyOwner = storyOwner,
    convertPromptUsed = convertPromptUsed,
    emotionMode = emotionMode,
    narrationApprovedAt = narrationApprovedAt,
    reviewNotes = reviewNotes,
    rejectMarkedAt = rejectMarkedAt
)
