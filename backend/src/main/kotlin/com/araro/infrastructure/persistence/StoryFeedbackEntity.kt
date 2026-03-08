package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_feedback")
class StoryFeedbackEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,
    @Column(name = "story_id")
    val storyId: Long? = null,
    @Column(name = "story_source", length = 20)
    val storySource: String? = "generated",
    @Column
    val rating: Int? = null,
    @Column(columnDefinition = "TEXT")
    val comment: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
