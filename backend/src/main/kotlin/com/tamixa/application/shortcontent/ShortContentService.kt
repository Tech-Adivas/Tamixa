package com.tamixa.application.shortcontent

import com.tamixa.api.admin.dto.ShortContentDto
import com.tamixa.api.shortcontent.dto.ShortContentResponse
import com.tamixa.application.port.ShortContentRepositoryPort
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.story.ModerationContext
import com.tamixa.application.story.StoryModerationService
import com.tamixa.domain.ShortContent
import com.tamixa.domain.ShortContentType
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class ShortContentService(
    private val repository: ShortContentRepositoryPort,
    private val storyModeration: StoryModerationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val STATUS_PUBLISHED = "PUBLISHED"
    }

    /**
     * Returns published short content of the given type and language (paginated).
     */
    fun findByTypeAndLanguage(type: String, language: String, page: Int, size: Int): List<ShortContentResponse> {
        val canonicalType = ShortContentType.fromString(type)?.value ?: type.trim().uppercase()
        val lang = language.trim().lowercase()
        log.debug("ShortContent list type={} language={} page={} size={}", canonicalType, lang, page, size)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val result = repository.findByTypeAndLanguageAndStatus(canonicalType, lang, STATUS_PUBLISHED, pageable)
        return result.content.map { toResponse(it) }
    }

    /**
     * Returns the published item for a given display date and type (e.g. "thought for the day" for today).
     */
    fun findDaily(type: String, language: String, date: LocalDate?): ShortContentResponse? {
        val canonicalType = ShortContentType.fromString(type)?.value ?: type.trim().uppercase()
        val lang = language.trim().lowercase()
        val d = date ?: LocalDate.now()
        log.debug("ShortContent daily type={} language={} date={}", canonicalType, lang, d)
        val item = repository.findByDisplayDateAndTypeAndLanguageAndStatus(d, canonicalType, lang, STATUS_PUBLISHED)
        return item?.let { toResponse(it) }
    }

    /**
     * Returns all supported short content types for the app (e.g. for tabs or filters).
     */
    fun getTypes(): List<String> = ShortContentType.allValues()

    fun listForAdmin(type: String?, language: String?, status: String?, page: Int, size: Int): Page<ShortContentDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val t = type?.trim()?.takeIf { it.isNotBlank() }
        val lang = language?.trim()?.takeIf { it.isNotBlank() }
        val st = status?.trim()?.takeIf { it.isNotBlank() }
        return repository.findForAdmin(t, lang, st, pageable).map { toDto(it) }
    }

    fun findByIdForAdmin(id: Long): ShortContentDto? =
        repository.findById(id)?.let { toDto(it) }

    fun create(
        type: String,
        content: String,
        answer: String?,
        language: String,
        ageMin: Int?,
        ageMax: Int?,
        displayDate: LocalDate?,
        audioUrl: String?,
        status: String
    ): ShortContentDto {
        val canonicalType = ShortContentType.fromString(type)?.value ?: type.trim().uppercase()
        val lang = language.trim().lowercase()
        moderateContent(content, answer, lang, "short-content-create")
        val now = Instant.now()
        val domain = ShortContent(
            id = 0,
            type = canonicalType,
            content = content.trim(),
            answer = answer?.trim()?.takeIf { it.isNotBlank() },
            language = lang,
            ageMin = ageMin,
            ageMax = ageMax,
            displayDate = displayDate,
            audioUrl = audioUrl?.trim()?.takeIf { it.isNotBlank() },
            status = status.trim().uppercase().takeIf { it in listOf("DRAFT", "PUBLISHED") } ?: "DRAFT",
            createdAt = now,
            updatedAt = now
        )
        val saved = repository.save(domain)
        return toDto(saved)
    }

    fun update(
        id: Long,
        type: String?,
        content: String?,
        answer: String?,
        language: String?,
        ageMin: Int?,
        ageMax: Int?,
        displayDate: LocalDate?,
        audioUrl: String?,
        status: String?
    ): ShortContentDto? {
        val existing = repository.findById(id) ?: return null
        val finalContent = content?.trim()?.takeIf { it.isNotBlank() } ?: existing.content
        val finalAnswer = answer?.trim()?.takeIf { it.isNotBlank() }
            ?: (existing.answer?.takeIf { it.isNotBlank() })
        val finalLang = language?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: existing.language
        moderateContent(finalContent, finalAnswer, finalLang, "short-content-update-$id")
        val now = Instant.now()
        val updated = existing.copy(
            type = type?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: existing.type,
            content = content?.trim()?.takeIf { it.isNotBlank() } ?: existing.content,
            answer = answer?.trim()?.takeIf { it.isNotBlank() } ?: existing.answer,
            language = language?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: existing.language,
            ageMin = ageMin,
            ageMax = ageMax,
            displayDate = displayDate,
            audioUrl = audioUrl?.trim()?.takeIf { it.isNotBlank() } ?: existing.audioUrl,
            status = status?.trim()?.uppercase()?.takeIf { it in listOf("DRAFT", "PUBLISHED") } ?: existing.status,
            updatedAt = now
        )
        val saved = repository.save(updated)
        return toDto(saved)
    }

    fun delete(id: Long): Boolean {
        if (repository.findById(id) == null) return false
        repository.deleteById(id)
        return true
    }

    /** Runs content moderation before save. Throws [ContentModerationException] if content is not child-safe. */
    private fun moderateContent(content: String, answer: String?, language: String, promptId: String) {
        val textToModerate = buildString {
            append(content.trim())
            answer?.trim()?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
        }
        if (textToModerate.isBlank()) return
        storyModeration.moderateBeforeSave(
            textToModerate,
            ModerationContext(promptId = promptId, language = language, age = 7)
        )
    }

    private fun toDto(d: ShortContent): ShortContentDto = ShortContentDto(
        id = d.id,
        type = d.type,
        content = d.content,
        answer = d.answer,
        language = d.language,
        ageMin = d.ageMin,
        ageMax = d.ageMax,
        displayDate = d.displayDate,
        audioUrl = d.audioUrl,
        status = d.status,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt
    )

    private fun toResponse(d: ShortContent): ShortContentResponse = ShortContentResponse(
        id = d.id,
        type = d.type,
        content = d.content,
        answer = d.answer,
        language = d.language,
        ageMin = d.ageMin,
        ageMax = d.ageMax,
        displayDate = d.displayDate,
        audioUrl = d.audioUrl,
        createdAt = d.createdAt
    )
}
