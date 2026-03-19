package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.GeneratedStorySceneRepositoryPort
import com.tamixa.application.storyengine.StorySceneExtractor
import com.tamixa.domain.narration.EmotionTag
import com.tamixa.domain.GeneratedStoryScene
import com.tamixa.domain.narration.EmotionTaggedScript
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GeneratedStorySceneRepositoryAdapter(
    private val sceneJpaRepository: GeneratedStorySceneJpaRepository,
    private val segmentJpaRepository: GeneratedStorySegmentJpaRepository,
    private val sceneExtractor: StorySceneExtractor
) : GeneratedStorySceneRepositoryPort {

    override fun saveSceneWithSegments(
        storyId: Long,
        language: String,
        script: String,
        totalDurationSeconds: Int,
        audioUrl: String,
        theme: String?,
        emotionTaggedScript: EmotionTaggedScript?
    ) {
        sceneJpaRepository.deleteByStoryIdAndLanguage(storyId, language)
        val sceneChunks = sceneExtractor.extractScenes(script)
        val backgroundHint = sceneExtractor.themeToBackgroundHint(theme)

        val (segmentsByScene, totalWordCount) = if (emotionTaggedScript != null) {
            assignEmotionSegmentsToScenes(script, sceneChunks, emotionTaggedScript)
        } else {
            simpleParagraphSegmentsByScene(script, sceneChunks)
        }
        val totalMs = totalDurationSeconds * 1000L

        sceneChunks.forEachIndexed { i, _ ->
            val segInfos = segmentsByScene.getOrNull(i) ?: emptyList()
            if (segInfos.isEmpty()) return@forEachIndexed
            val sceneWordCount = segInfos.sumOf { it.wordCount }
            val sceneDurationMs = if (totalWordCount > 0) (totalMs * sceneWordCount) / totalWordCount else totalMs / sceneChunks.size.coerceAtLeast(1)
            val durationPerSegment = if (segInfos.size > 1) sceneDurationMs / segInfos.size else sceneDurationMs
            val sceneEntity = GeneratedStorySceneEntity(
                storyId = storyId,
                language = language,
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
                GeneratedStorySegmentEntity(
                    sceneId = savedScene.id,
                    segmentIndex = j,
                    speaker = info.speaker,
                    text = info.text,
                    durationMs = segMs.coerceAtLeast(1),
                    audioUrl = audioUrl,
                    segmentType = info.segmentType
                )
            }
            segmentJpaRepository.saveAll(segmentEntities)
        }
    }

    @Transactional(readOnly = true)
    override fun findScenesByStoryIdAndLanguage(storyId: Long, language: String): List<GeneratedStoryScene> {
        val scenes = sceneJpaRepository.findByStoryIdAndLanguageOrderBySceneIndex(storyId, language)
        return scenes.map { scene ->
            val segments = segmentJpaRepository.findBySceneIdOrderBySegmentIndex(scene.id).map { it.toDomain() }
            GeneratedStoryScene(
                id = scene.id,
                storyId = scene.storyId,
                language = scene.language,
                sceneIndex = scene.sceneIndex,
                backgroundHint = scene.backgroundHint,
                createdAt = scene.createdAt,
                segments = segments
            )
        }
    }

    private data class SegmentInfo(val text: String, val speaker: String, val segmentType: String, val wordCount: Int)

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
                wordCount = seg.wordCount()
            )
        }
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
                SegmentInfo(text, "Narrator", "NARRATION", wc)
            }.ifEmpty { listOf(SegmentInfo(chunk.text.ifBlank { "..." }, "Narrator", "NARRATION", 1)) }
        }
        return result to totalWords.coerceAtLeast(1)
    }
}
