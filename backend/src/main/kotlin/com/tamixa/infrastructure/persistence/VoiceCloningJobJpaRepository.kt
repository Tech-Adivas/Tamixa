package com.tamixa.infrastructure.persistence

import com.tamixa.domain.VoiceCloningStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface VoiceCloningJobJpaRepository : JpaRepository<VoiceCloningJobEntity, Long> {
    fun findByParentId(parentId: Long): List<VoiceCloningJobEntity>
    fun findByStatus(status: VoiceCloningStatus): List<VoiceCloningJobEntity>

    @Modifying
    @Query(
        value = """
        UPDATE voice_cloning_jobs
        SET status = :status,
            eleven_labs_voice_id = COALESCE(:elevenLabsVoiceId, eleven_labs_voice_id),
            google_voice_cloning_key = COALESCE(:googleVoiceCloningKey, google_voice_cloning_key),
            error_message = :errorMessage,
            completed_at = :now
        WHERE id = :id
        """,
        nativeQuery = true
    )
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: String,
        @Param("elevenLabsVoiceId") elevenLabsVoiceId: String?,
        @Param("googleVoiceCloningKey") googleVoiceCloningKey: String?,
        @Param("errorMessage") errorMessage: String?,
        @Param("now") now: Instant
    )

    @Modifying
    @Query(value = "DELETE FROM voice_cloning_jobs WHERE parent_id = :parentId", nativeQuery = true)
    fun deleteByParentId(@Param("parentId") parentId: Long)
}
