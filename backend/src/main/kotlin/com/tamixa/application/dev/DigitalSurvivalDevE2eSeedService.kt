package com.tamixa.application.dev

import com.tamixa.api.dev.DevDigitalSurvivalParentLibraryPostProcessorImpl.Companion.DIGITAL_SURVIVAL_STORY_OWNER
import com.tamixa.api.dev.DevDigitalSurvivalParentLibraryPostProcessorImpl.Companion.PLACEHOLDER_AUDIO_PATH
import com.tamixa.domain.TranslationPipelineStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.infrastructure.persistence.LibraryStoryEntity
import com.tamixa.infrastructure.persistence.LibraryStoryJpaRepository
import com.tamixa.infrastructure.persistence.StoryNarrationAudioEntity
import com.tamixa.infrastructure.persistence.StoryNarrationAudioJpaRepository
import com.tamixa.infrastructure.persistence.StoryTranslationEntity
import com.tamixa.infrastructure.persistence.StoryTranslationJpaRepository
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Idempotently prepares Digital Survival Flyway seeds for the **parent library API** on a dev machine:
 * [LibraryStoryEntity], matching [story_translations] + [story_narration_audio] rows the JPA catalog queries require,
 * plus PUBLISHED + narration approval + placeholder master audio path.
 *
 * **Never** register this bean outside `dev` profile.
 */
@Service
@Profile("dev")
class DigitalSurvivalDevE2eSeedService(
    private val libraryStoryJpaRepository: LibraryStoryJpaRepository,
    private val storyTranslationJpaRepository: StoryTranslationJpaRepository,
    private val storyNarrationAudioJpaRepository: StoryNarrationAudioJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class PrepareResult(
        val storiesUpdated: Int,
        val translationsCreated: Int,
        val narrationsUpserted: Int,
    )

    @Transactional
    fun prepareE2eSeed(): PrepareResult {
        val masters = libraryStoryJpaRepository.findAllActiveByStoryOwner(DIGITAL_SURVIVAL_STORY_OWNER)
        if (masters.isEmpty()) {
            log.warn("No active library_stories with story_owner={}", DIGITAL_SURVIVAL_STORY_OWNER)
            return PrepareResult(0, 0, 0)
        }
        var translationsCreated = 0
        var narrationsUpserted = 0
        val now = Instant.now()
        for (master in masters) {
            upsertMasterForDelivery(master, now)
            val lang = master.language.trim().lowercase().take(10)
            val translation =
                storyTranslationJpaRepository.findByMasterStoryIdAndLanguage(master.id, lang)
                    ?: createTranslation(master, lang).also { translationsCreated++ }
            upsertPlaceholderNarration(translation.id).also { if (it) narrationsUpserted++ }
        }
        log.info(
            "Digital Survival dev E2E: updated {} masters; translationsCreated={} narrationsUpserted={}",
            masters.size,
            translationsCreated,
            narrationsUpserted,
        )
        return PrepareResult(masters.size, translationsCreated, narrationsUpserted)
    }

    private fun upsertMasterForDelivery(master: LibraryStoryEntity, now: Instant) {
        master.status = "PUBLISHED"
        master.narrationApprovedAt = now
        master.updatedAt = now
        if (master.audioFileUrl.isNullOrBlank()) {
            master.audioFileUrl = PLACEHOLDER_AUDIO_PATH
        }
        libraryStoryJpaRepository.save(master)
    }

    private fun createTranslation(master: LibraryStoryEntity, lang: String): StoryTranslationEntity {
        val body = master.content.ifBlank { " " }
        val entity =
            StoryTranslationEntity(
                masterStoryId = master.id,
                language = lang,
                title = master.title,
                content = body,
                moral = master.moral,
                wordCount = master.wordCount,
                readingTimeMinutes = master.readingTimeMinutes,
                createdAt = Instant.now(),
                status = TranslationPipelineStatus.COMPLETED,
            )
        return storyTranslationJpaRepository.save(entity)
    }

    /**
     * @return true if a save or replace occurred.
     */
    private fun upsertPlaceholderNarration(translationId: Long): Boolean {
        val existing = storyNarrationAudioJpaRepository.findByTranslationIdAndVoiceProfile(translationId, "default")
        if (existing != null &&
            existing.status == NarrationAudioStatus.READY &&
            existing.audioUrl == PLACEHOLDER_AUDIO_PATH
        ) {
            return false
        }
        if (existing != null) {
            storyNarrationAudioJpaRepository.delete(existing)
        }
        storyNarrationAudioJpaRepository.save(
            StoryNarrationAudioEntity(
                translationId = translationId,
                voiceProfile = "default",
                audioUrl = PLACEHOLDER_AUDIO_PATH,
                durationSeconds = 1,
                status = NarrationAudioStatus.READY,
                createdAt = Instant.now(),
                truncationWarning = false,
            ),
        )
        return true
    }
}
