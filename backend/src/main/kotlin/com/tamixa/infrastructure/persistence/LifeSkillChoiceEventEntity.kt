package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "life_skill_choice_events")
class LifeSkillChoiceEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "parent_id", nullable = false)
    val parentId: Long,

    @Column(name = "child_id", nullable = false)
    val childId: Long,

    @Column(name = "library_story_id", nullable = false)
    val libraryStoryId: Long,

    @Column(name = "segment_id", nullable = false, length = 128)
    val segmentId: String,

    @Column(name = "choice_id", nullable = false, length = 128)
    val choiceId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_deltas", nullable = false, columnDefinition = "jsonb")
    val skillDeltas: Map<String, Int> = emptyMap(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
