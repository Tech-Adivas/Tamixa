package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_segments")
class StorySegmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "scene_id", nullable = false)
    val sceneId: Long,

    @Column(name = "segment_index", nullable = false)
    val segmentIndex: Int = 0,

    @Column(nullable = false, length = 64)
    val speaker: String = "Narrator",

    @Column(name = "segment_type", length = 32)
    val segmentType: String = "NARRATION",

    @Column(nullable = false, columnDefinition = "TEXT")
    val text: String,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long = 0L,

    @Column(name = "audio_url", length = 512)
    val audioUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = com.tamixa.domain.StorySegment(
        id = id,
        sceneId = sceneId,
        segmentIndex = segmentIndex,
        speaker = speaker,
        text = text,
        durationMs = durationMs,
        audioUrl = audioUrl,
        segmentType = segmentType,
        createdAt = createdAt
    )
}
