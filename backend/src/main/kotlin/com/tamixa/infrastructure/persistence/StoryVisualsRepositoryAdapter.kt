package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.visuals.StoryVisualsRepositoryPort
import com.tamixa.domain.visuals.StoryVisualImage
import com.tamixa.domain.visuals.StoryVisualSet
import com.tamixa.domain.visuals.StoryVisualShot
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class StoryVisualsRepositoryAdapter(
    private val sets: StoryVisualSetJpaRepository,
    private val images: StoryVisualImageJpaRepository,
    private val shots: StoryVisualShotJpaRepository,
) : StoryVisualsRepositoryPort {

    override fun findSetByStoryId(storyId: Long): StoryVisualSet? = sets.findByStoryId(storyId)?.toDomain()

    override fun saveSet(set: StoryVisualSet): StoryVisualSet = sets.save(
        StoryVisualSetEntity(
            id = set.id,
            storyId = set.storyId,
            status = set.status,
            sourceLanguage = set.sourceLanguage,
            sourceWordCount = set.sourceWordCount,
            charactersJson = set.charactersJson,
            characterSheetPath = set.characterSheetPath,
            lastError = set.lastError?.take(1000),
            approvedAt = set.approvedAt,
            createdAt = set.createdAt,
            updatedAt = Instant.now(),
        )
    ).toDomain()

    override fun findImages(setId: Long): List<StoryVisualImage> =
        images.findBySetIdOrderByImageKey(setId).map { it.toDomain() }

    override fun saveImage(image: StoryVisualImage): StoryVisualImage = images.save(image.toEntity()).toDomain()

    override fun findShots(setId: Long): List<StoryVisualShot> =
        shots.findBySetIdOrderByShotIndex(setId).map { it.toDomain() }

    @Transactional
    override fun replacePlan(setId: Long, images: List<StoryVisualImage>, shots: List<StoryVisualShot>) {
        this.shots.deleteBySetIdQuery(setId)
        this.images.deleteBySetIdQuery(setId)
        this.images.saveAll(images.map { it.copy(id = 0, setId = setId).toEntity() })
        this.shots.saveAll(shots.map { it.copy(id = 0, setId = setId).toEntity() })
    }

    @Transactional
    override fun deleteSetByStoryId(storyId: Long) {
        sets.deleteByStoryIdQuery(storyId)
    }
}

private fun StoryVisualSetEntity.toDomain() = StoryVisualSet(
    id = id,
    storyId = storyId,
    status = status,
    sourceLanguage = sourceLanguage,
    sourceWordCount = sourceWordCount,
    charactersJson = charactersJson,
    characterSheetPath = characterSheetPath,
    lastError = lastError,
    approvedAt = approvedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun StoryVisualImageEntity.toDomain() = StoryVisualImage(
    id = id,
    setId = setId,
    imageKey = imageKey,
    storyMoment = storyMoment,
    prompt = prompt,
    subjectsJson = subjectsJson,
    imagePath = imagePath,
    status = status,
    lastError = lastError,
)

private fun StoryVisualImage.toEntity() = StoryVisualImageEntity(
    id = id,
    setId = setId,
    imageKey = imageKey,
    storyMoment = storyMoment,
    prompt = prompt,
    subjectsJson = subjectsJson,
    imagePath = imagePath,
    status = status,
    lastError = lastError?.take(1000),
    updatedAt = Instant.now(),
)

private fun StoryVisualShotEntity.toDomain() = StoryVisualShot(
    id = id,
    setId = setId,
    shotIndex = shotIndex,
    imageKey = imageKey,
    narrationStart = narrationStart,
    startWord = startWord,
    camera = camera,
    focus = focus,
    movement = movement,
)

private fun StoryVisualShot.toEntity() = StoryVisualShotEntity(
    id = id,
    setId = setId,
    shotIndex = shotIndex,
    imageKey = imageKey,
    narrationStart = narrationStart,
    startWord = startWord,
    camera = camera,
    focus = focus,
    movement = movement,
)
