package com.tamixa.infrastructure.persistence

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
@Table(name = "voice_clone_analytics")
class VoiceCloneAnalyticsEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: String,

    @Column(name = "voice_profile_id")
    val voiceProfileId: Long? = null,

    @Column(name = "voice_cloning_job_id")
    val voiceCloningJobId: Long? = null,

    @Column(name = "story_id")
    val storyId: Long? = null,

    @Column(name = "story_source", length = 20)
    val storySource: String? = null,

    @Column(length = 10)
    val language: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
