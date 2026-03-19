package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "short_content")
class ShortContentEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 50)
    var type: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(columnDefinition = "TEXT")
    var answer: String? = null,

    @Column(nullable = false, length = 10)
    var language: String = "ta",

    @Column(name = "age_min")
    var ageMin: Int? = null,

    @Column(name = "age_max")
    var ageMax: Int? = null,

    @Column(name = "display_date")
    var displayDate: LocalDate? = null,

    @Column(name = "audio_url", length = 512)
    var audioUrl: String? = null,

    @Column(nullable = false, length = 20)
    var status: String = "DRAFT",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
