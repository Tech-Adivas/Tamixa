package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_scenes")
class StorySceneEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "translation_id", nullable = false)
    val translationId: Long,

    @Column(name = "scene_index", nullable = false)
    val sceneIndex: Int = 0,

    @Column(name = "background_hint", length = 255)
    val backgroundHint: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain(segments: List<com.tamixa.domain.StorySegment> = emptyList()) = com.tamixa.domain.StoryScene(
        id = id,
        translationId = translationId,
        sceneIndex = sceneIndex,
        backgroundHint = backgroundHint,
        createdAt = createdAt,
        segments = segments
    )
}
