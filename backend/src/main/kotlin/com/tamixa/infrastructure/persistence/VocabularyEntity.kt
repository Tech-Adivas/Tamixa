package com.tamixa.infrastructure.persistence

import com.tamixa.domain.ChildVocabulary
import com.tamixa.domain.VocabularyWord
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
@Table(name = "vocabulary_words")
class VocabularyWordEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    val word: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val definition: String,

    @Column(nullable = false, length = 10)
    val language: String,

    @Column(name = "difficulty_level", nullable = false)
    val difficultyLevel: Int = 1,

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    val exampleSentence: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = VocabularyWord(
        id = id,
        word = word,
        definition = definition,
        language = language,
        difficultyLevel = difficultyLevel,
        exampleSentence = exampleSentence,
        createdAt = createdAt
    )
}

@Entity
@Table(name = "child_vocabulary")
class ChildVocabularyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    val child: ChildEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    val word: VocabularyWordEntity,

    @Column(name = "mastery_level", nullable = false)
    val masteryLevel: Int = ChildVocabulary.MASTERY_LEARNING,

    @Column(name = "learned_at", nullable = false)
    val learnedAt: Instant = Instant.now()
) {
    fun toDomain() = ChildVocabulary(
        id = id,
        childId = child.id,
        wordId = word.id,
        masteryLevel = masteryLevel,
        learnedAt = learnedAt
    )
}
