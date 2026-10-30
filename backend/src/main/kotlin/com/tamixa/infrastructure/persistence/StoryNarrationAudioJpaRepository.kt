package com.tamixa.infrastructure.persistence

import com.tamixa.domain.narration.NarrationAudioStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface StoryNarrationAudioJpaRepository : JpaRepository<StoryNarrationAudioEntity, Long> {

    fun findByTranslationId(translationId: Long): StoryNarrationAudioEntity?

    fun findAllByVoiceProfileIn(voiceProfiles: Collection<String>): List<StoryNarrationAudioEntity>

    fun findAllByVoiceProfileStartingWith(prefix: String): List<StoryNarrationAudioEntity>

    fun findAllByTranslationId(translationId: Long): List<StoryNarrationAudioEntity>

    fun findAllByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): List<StoryNarrationAudioEntity>

    fun findByTranslationIdAndVoiceProfile(translationId: Long, voiceProfile: String): StoryNarrationAudioEntity?

    fun findByTranslationIdInAndVoiceProfile(translationIds: List<Long>, voiceProfile: String): List<StoryNarrationAudioEntity>

    fun existsByTranslationIdAndStatus(translationId: Long, status: NarrationAudioStatus): Boolean

    /** Prevents duplicate generation: true if READY audio exists for this (translation, voice) pair. */
    fun existsByTranslationIdAndVoiceProfileAndStatus(
        translationId: Long,
        voiceProfile: String,
        status: NarrationAudioStatus
    ): Boolean

    /**
     * Explicit bulk deletes in their own write transaction. Derived `deleteBy…` methods only remove rows inside a
     * writable transaction; callers such as `StoryLibraryService.invalidateNarrationAudioForLanguages` run them through a
     * self-invocation (their @Transactional is bypassed), so the rows silently stayed READY while their S3 files were
     * deleted, and players got 404s.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from StoryNarrationAudioEntity a where a.translationId = :translationId")
    fun deleteByTranslationId(@Param("translationId") translationId: Long): Int

    /** Delete single narration entry for a voice profile (e.g. cloned:1) so it can be regenerated. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from StoryNarrationAudioEntity a where a.translationId = :translationId and a.voiceProfile = :voiceProfile")
    fun deleteByTranslationIdAndVoiceProfile(
        @Param("translationId") translationId: Long,
        @Param("voiceProfile") voiceProfile: String,
    ): Int
}
