package com.tamixa.infrastructure.persistence

import com.tamixa.domain.narration.ToneMode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "story_narration_scripts")
class StoryNarrationScriptEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "translation_id", nullable = false, unique = true)
    val translationId: Long,

    @Column(name = "tone_mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val toneMode: ToneMode = ToneMode.CALM,

    @Column(name = "script_text", nullable = false, columnDefinition = "TEXT")
    val scriptText: String,

    @Column(name = "safety_score", nullable = false)
    val safetyScore: Int = 100,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
