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
@Table(name = "story_analytics")
class StoryAnalyticsEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(name = "story_id", nullable = false)
    val storyId: Long,

    @Column(name = "story_source", nullable = false, length = 20)
    val storySource: String = "generated",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    val child: ChildEntity? = null,

    @Column(nullable = false, length = 10)
    val language: String,

    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: String,

    @Column(nullable = false)
    val timestamp: Instant = Instant.now(),

    @Column(name = "playback_position_seconds", nullable = false)
    val playbackPositionSeconds: Int = 0
)
