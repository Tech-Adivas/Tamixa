package com.araro.infrastructure.persistence

import com.araro.domain.VoiceCloningStatus
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
            eleven_labs_voice_id = :elevenLabsVoiceId,
            error_message = :errorMessage,
            completed_at = :now
        WHERE id = :id
        """,
        nativeQuery = true
    )
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: VoiceCloningStatus,
        @Param("elevenLabsVoiceId") elevenLabsVoiceId: String?,
        @Param("errorMessage") errorMessage: String?,
        @Param("now") now: Instant
    )
}
