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
import java.time.LocalDate

@Entity
@Table(name = "children")
class ChildEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(name = "date_of_birth", nullable = false)
    val dateOfBirth: LocalDate,

    @Column(name = "language_preference", length = 10)
    val languagePreference: String? = null,

    @Column(length = 500)
    val interests: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "favorite_color", length = 50)
    val favoriteColor: String? = null,

    @Column(name = "favorite_animal", length = 100)
    val favoriteAnimal: String? = null,

    @Column(name = "character_traits", length = 500)
    val characterTraits: String? = null,

    @Column(name = "avatar_choice", length = 50)
    val avatarChoice: String? = null
)
