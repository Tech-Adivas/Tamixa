package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.SegmentInfoForTts
import com.tamixa.application.port.StorySceneRepositoryPort
import com.tamixa.application.storyengine.StorySceneExtractor
import com.tamixa.domain.StoryScene
import com.tamixa.domain.narration.EmotionTag
import com.tamixa.domain.narration.EmotionTaggedScript
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StorySceneRepositoryAdapter(
    private val sceneJpaRepository: StorySceneJpaRepository,
    private val segmentJpaRepository: StorySegmentJpaRepository,
    private val sceneExtractor: StorySceneExtractor,
    private val entityManager: EntityManager
) : StorySceneRepositoryPort {

    override fun computeSegmentInfosForTts(
        script: String,
        theme: String?,
        emotionTaggedScript: EmotionTaggedScript?
    ): List<SegmentInfoForTts> {
        val sceneChunks = sceneExtractor.extractScenes(script)
        val segmentsByScene = if (emotionTaggedScript != null) {
            assignEmotionSegmentsToScenes(script, sceneChunks, emotionTaggedScript).first
        } else {
            simpleParagraphSegmentsByScene(script, sceneChunks).first
        }
        return segmentsByScene.flatMap { sceneSegs ->
            sceneSegs.map { info -> SegmentInfoForTts(text = info.text, emotion = info.emotion) }
        }
    }

    override fun saveSceneWithSegments(
        translationId: Long,
        script: String,
        totalDurationSeconds: Int,
        audioUrl: String,
        theme: String?,
        emotionTaggedScript: EmotionTaggedScript?,
        segmentAudioUrls: List<String>?
    ) {
        deleteExistingScenes(translationId)
        // Flush so DELETEs are executed before INSERTs; avoids unique constraint on (translation_id, scene_index) on regenerate.
        entityManager.flush()
        val sceneChunks = sceneExtractor.extractScenes(script)
        val backgroundHint = sceneExtractor.themeToBackgroundHint(theme)

        val (segmentsByScene, totalWordCount) = if (emotionTaggedScript != null) {
            assignEmotionSegmentsToScenes(script, sceneChunks, emotionTaggedScript)
        } else {
            simpleParagraphSegmentsByScene(script, sceneChunks)
        }
        val totalMs = totalDurationSeconds * 1000L
        var globalSegmentIndex = 0

        sceneChunks.forEachIndexed { i, _ ->
            val segInfos = segmentsByScene.getOrNull(i) ?: emptyList()
            if (segInfos.isEmpty()) return@forEachIndexed
            val sceneWordCount = segInfos.sumOf { it.wordCount }
            val sceneDurationMs = if (totalWordCount > 0) (totalMs * sceneWordCount) / totalWordCount else totalMs / sceneChunks.size.coerceAtLeast(1)
            val durationPerSegment = if (segInfos.size > 1) sceneDurationMs / segInfos.size else sceneDurationMs
            val sceneEntity = StorySceneEntity(
                translationId = translationId,
                sceneIndex = i,
                backgroundHint = if (i == 0) backgroundHint else null
            )
            val savedScene = sceneJpaRepository.save(sceneEntity)
            val segmentEntities = segInfos.mapIndexed { j, info ->
                val segMs = if (totalWordCount > 0 && sceneWordCount > 0) {
                    (totalMs * info.wordCount) / totalWordCount
                } else {
                    durationPerSegment
                }
                val segUrl = segmentAudioUrls?.getOrNull(globalSegmentIndex++) ?: audioUrl
                StorySegmentEntity(
                    sceneId = savedScene.id,
                    segmentIndex = j,
                    speaker = info.speaker,
                    text = info.text,
                    durationMs = segMs.coerceAtLeast(1),
                    audioUrl = segUrl,
                    segmentType = info.segmentType
                )
            }
            segmentJpaRepository.saveAll(segmentEntities)
        }
    }

    private data class SegmentInfo(
        val text: String,
        val speaker: String,
        val segmentType: String,
        val wordCount: Int,
        val emotion: EmotionTag = EmotionTag.CALM
    )

    private fun deleteExistingScenes(translationId: Long) {
        val existing = sceneJpaRepository.findByTranslationIdOrderBySceneIndex(translationId)
        existing.forEach { scene ->
            segmentJpaRepository.deleteBySceneId(scene.id)
            sceneJpaRepository.delete(scene)
        }
    }

    private fun assignEmotionSegmentsToScenes(
        script: String,
        sceneChunks: List<com.tamixa.application.storyengine.SceneChunk>,
        emotionTagged: EmotionTaggedScript
    ): Pair<List<List<SegmentInfo>>, Int> {
        if (emotionTagged.segments.isEmpty()) return simpleParagraphSegmentsByScene(script, sceneChunks)
        val totalWordCount = emotionTagged.segments.sumOf { it.wordCount() }
        val segmentInfos = emotionTagged.segments.map { seg ->
            SegmentInfo(
                text = seg.text,
                speaker = if (seg.emotion == EmotionTag.DIALOGUE) "Character" else "Narrator",
                segmentType = if (seg.emotion == EmotionTag.DIALOGUE) "DIALOGUE" else "NARRATION",
                wordCount = seg.wordCount(),
                emotion = seg.emotion
            )
        }
        // Assign segments to scenes by character offset in script
        var segIdx = 0
        var charPos = 0
        val sceneEndOffsets = sceneChunks.runningFold(0) { acc, c -> acc + c.text.length }.drop(1)
        val result = mutableListOf<List<SegmentInfo>>()
        for (i in sceneChunks.indices) {
            val endOffset = sceneEndOffsets.getOrElse(i) { Int.MAX_VALUE }
            val list = mutableListOf<SegmentInfo>()
            while (segIdx < segmentInfos.size) {
                val seg = segmentInfos[segIdx]
                if (charPos >= endOffset) break
                list.add(seg)
                charPos += seg.text.length
                segIdx++
            }
            result.add(list)
        }
        return result to totalWordCount
    }

    private fun simpleParagraphSegmentsByScene(
        script: String,
        sceneChunks: List<com.tamixa.application.storyengine.SceneChunk>
    ): Pair<List<List<SegmentInfo>>, Int> {
        var totalWords = 0
        val result = sceneChunks.map { chunk ->
            val paragraphs = chunk.text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
            paragraphs.map { text ->
                val wc = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                totalWords += wc
                SegmentInfo(text, "Narrator", "NARRATION", wc, EmotionTag.CALM)
            }.ifEmpty { listOf(SegmentInfo(chunk.text.ifBlank { "..." }, "Narrator", "NARRATION", 1, EmotionTag.CALM)) }
        }
        return result to totalWords.coerceAtLeast(1)
    }

    @Transactional(readOnly = true)
    override fun findScenesByTranslationId(translationId: Long): List<StoryScene> {
        val scenes = sceneJpaRepository.findByTranslationIdOrderBySceneIndex(translationId)
        return scenes.map { scene ->
            val segments = segmentJpaRepository.findBySceneIdOrderBySegmentIndex(scene.id).map { it.toDomain() }
            scene.toDomain(segments = segments)
        }
    }

    override fun deleteByTranslationId(translationId: Long) {
        deleteExistingScenes(translationId)
    }

    override fun deleteAll(): Int {
        val count = sceneJpaRepository.count().toInt()
        sceneJpaRepository.findAll().forEach { scene ->
            segmentJpaRepository.deleteBySceneId(scene.id)
            sceneJpaRepository.delete(scene)
        }
        return count
    }
}
