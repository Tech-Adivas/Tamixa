package com.tamixa.application.playback

import com.tamixa.application.port.GeneratedStorySceneRepositoryPort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.StorySceneRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.stream.AudioStreamService
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.application.stream.NarrationScriptService
import com.tamixa.application.stream.StreamLanguageUtils
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.domain.narration.NarrationAudioStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Builds playback manifest / timeline for story playback.
 * Per Tamixa spec: scenes with segments (text, audioUrl, durationMs).
 * MVP: one scene, segments from script paragraphs; single audio URL for full story.
 */
@Service
class PlaybackManifestService(
    private val audioStreamService: AudioStreamService,
    private val narrationScriptService: NarrationScriptService,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val storySceneRepository: StorySceneRepositoryPort,
    private val generatedStorySceneRepository: GeneratedStorySceneRepositoryPort,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getPlaybackManifest(
        storyId: Long,
        language: String,
        storySource: String?,
        voiceProfile: String?,
        parentId: Long?
    ): PlaybackManifest? {
        val effectiveLang = StreamLanguageUtils.normalize(language)
        val source = storySource?.takeIf { it in listOf("library", "generated") }
            ?: if (storyLibraryRepository.findById(storyId) != null) "library" else "generated"

        return when (source) {
            "library" -> getLibraryManifest(storyId, effectiveLang, voiceProfile, parentId)
            else -> getGeneratedManifest(storyId, effectiveLang, parentId)
        }
    }

    private fun getLibraryManifest(
        storyId: Long,
        language: String,
        voiceProfile: String?,
        parentId: Long?
    ): PlaybackManifest? {
        val story = storyLibraryRepository.findById(storyId) ?: return null
        val audioUrl = when {
            voiceProfile != null && voiceProfile.equals("family", ignoreCase = true) && parentId != null ->
                audioStreamService.getFamilyVoiceStreamUrl(storyId, language, parentId)
            voiceProfile != null && voiceProfile.isNotBlank() ->
                audioStreamService.getLibraryNarrationStreamUrl(storyId, language, voiceProfile, parentId)
            else -> audioStreamService.getLibraryStreamUrl(storyId, language, parentId)
        } ?: return null

        val script = narrationScriptService.getNarrationScript(storyId, language) ?: return null
        val durationSeconds = resolveDurationSeconds(storyId, language)
        val (coverImageUrl, coverVideoUrl) = resolveCoverUrls(story.coverImageUrl, story.coverVideoUrl)
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language)
        val persistedScenes = translation?.let { storySceneRepository.findScenesByTranslationId(it.id) }
        return if (!persistedScenes.isNullOrEmpty() && persistedScenes.any { it.segments.isNotEmpty() }) {
            buildManifestFromScenes(
                storyId = storyId,
                title = story.title ?: story.theme ?: "Story",
                storySource = "library",
                audioUrl = audioUrl,
                scenes = persistedScenes,
                coverImageUrl = coverImageUrl,
                coverVideoUrl = coverVideoUrl
            )
        } else {
            buildManifest(
                storyId = storyId,
                title = story.title ?: story.theme ?: "Story",
                storySource = "library",
                audioUrl = audioUrl,
                script = script,
                durationSeconds = durationSeconds,
                coverImageUrl = coverImageUrl,
                coverVideoUrl = coverVideoUrl
            )
        }
    }

    private fun getGeneratedManifest(
        storyId: Long,
        language: String,
        parentId: Long?
    ): PlaybackManifest? {
        val story = storyRepository.findById(storyId) ?: return null
        val audioUrl = audioStreamService.getGeneratedStreamUrl(storyId, language, parentId) ?: return null
        val effectiveLang = StreamLanguageUtils.normalize(language)
        val persistedScenes = generatedStorySceneRepository.findScenesByStoryIdAndLanguage(storyId, effectiveLang)
        val (coverImageUrl, coverVideoUrl) = resolveCoverUrls(story.coverImageUrl, story.coverVideoUrl)
        return if (persistedScenes.isNotEmpty() && persistedScenes.any { it.segments.isNotEmpty() }) {
            buildManifestFromGeneratedScenes(
                storyId = storyId,
                title = story.title ?: story.theme ?: "Story",
                audioUrl = audioUrl,
                scenes = persistedScenes,
                coverImageUrl = coverImageUrl,
                coverVideoUrl = coverVideoUrl
            )
        } else {
            val script = narrationScriptService.getNarrationScript(storyId, language) ?: story.content
            val durationSeconds = (story.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
            val backgroundHint = story.theme?.takeIf { it.isNotBlank() }?.let { theme ->
                when {
                    theme.contains("adventure", ignoreCase = true) || theme.contains("forest", ignoreCase = true) -> "forest_adventure"
                    theme.contains("nature", ignoreCase = true) || theme.contains("garden", ignoreCase = true) -> "garden_day"
                    theme.contains("village", ignoreCase = true) || theme.contains("home", ignoreCase = true) -> "village_morning"
                    theme.contains("sea", ignoreCase = true) || theme.contains("ocean", ignoreCase = true) || theme.contains("beach", ignoreCase = true) -> "beach_sunset"
                    theme.contains("night", ignoreCase = true) || theme.contains("moon", ignoreCase = true) || theme.contains("star", ignoreCase = true) -> "night_stars"
                    theme.contains("animal", ignoreCase = true) || theme.contains("pet", ignoreCase = true) -> "meadow_animals"
                    theme.contains("family", ignoreCase = true) || theme.contains("friend", ignoreCase = true) -> "home_cozy"
                    else -> theme.replace(Regex("[^a-z0-9]"), "_").take(64).ifBlank { null }
                }
            }
            buildManifest(
                storyId = storyId,
                title = story.title ?: story.theme ?: "Story",
                storySource = "generated",
                audioUrl = audioUrl,
                script = script,
                durationSeconds = durationSeconds,
                coverImageUrl = coverImageUrl,
                coverVideoUrl = null,
                backgroundHint = backgroundHint
            )
        }
    }

    private fun buildManifestFromGeneratedScenes(
        storyId: Long,
        title: String,
        audioUrl: String,
        scenes: List<com.tamixa.domain.GeneratedStoryScene>,
        coverImageUrl: String?,
        coverVideoUrl: String? = null
    ): PlaybackManifest {
        var segCounter = 0
        val playbackScenes = scenes.mapIndexed { sceneIdx, scene ->
            PlaybackScene(
                sceneId = "scene_${sceneIdx + 1}",
                backgroundHint = scene.backgroundHint,
                segments = scene.segments.map { s ->
                    segCounter++
                    PlaybackSegment(
                        segmentId = "seg_$segCounter",
                        speaker = s.speaker,
                        text = s.text,
                        audioUrl = s.audioUrl ?: audioUrl,
                        durationMs = s.durationMs
                    )
                }
            )
        }
        val totalMs = playbackScenes.flatMap { it.segments }.sumOf { it.durationMs }
        return PlaybackManifest(
            storyId = "story_$storyId",
            title = title,
            storySource = "generated",
            audioUrl = audioUrl,
            coverImageUrl = coverImageUrl,
            coverVideoUrl = coverVideoUrl,
            scenes = playbackScenes,
            totalDurationMs = totalMs
        )
    }

    private fun resolveDurationSeconds(storyId: Long, language: String): Int {
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language) ?: run {
            val story = storyLibraryRepository.findById(storyId) ?: return 60
            return (story.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
        }
        val audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, "default")
        return when {
            audio != null && audio.status == NarrationAudioStatus.READY && audio.durationSeconds > 0 ->
                audio.durationSeconds
            else -> (translation.readingTimeMinutes * 60).toInt().coerceAtLeast(1)
        }
    }

    private fun resolveCoverUrls(coverImagePath: String?, coverVideoPath: String?): Pair<String?, String?> {
        val baseUrl = appProperties.audio.publicBaseUrl.trimEnd('/')
        val coverImageUrl = coverImagePath?.let { coverImageUrlResolver.resolveCoverPath(it) }
            ?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
        val coverVideoUrl = coverVideoPath?.let { coverImageUrlResolver.resolveCoverVideoPath(it) }
            ?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
        return coverImageUrl to coverVideoUrl
    }

    private fun buildManifestFromScenes(
        storyId: Long,
        title: String,
        storySource: String,
        audioUrl: String,
        scenes: List<com.tamixa.domain.StoryScene>,
        coverImageUrl: String? = null,
        coverVideoUrl: String? = null
    ): PlaybackManifest {
        var segCounter = 0
        val playbackScenes = scenes.mapIndexed { sceneIdx, scene ->
            PlaybackScene(
                sceneId = "scene_${sceneIdx + 1}",
                backgroundHint = scene.backgroundHint,
                segments = scene.segments.map { s ->
                    segCounter++
                    PlaybackSegment(
                        segmentId = "seg_$segCounter",
                        speaker = s.speaker,
                        text = s.text,
                        audioUrl = s.audioUrl ?: audioUrl,
                        durationMs = s.durationMs
                    )
                }
            )
        }
        val totalMs = playbackScenes.flatMap { it.segments }.sumOf { it.durationMs }
        return PlaybackManifest(
            storyId = "story_$storyId",
            title = title,
            storySource = storySource,
            audioUrl = audioUrl,
            coverImageUrl = coverImageUrl,
            coverVideoUrl = coverVideoUrl,
            scenes = playbackScenes,
            totalDurationMs = totalMs
        )
    }

    private fun buildManifest(
        storyId: Long,
        title: String,
        storySource: String,
        audioUrl: String,
        script: String,
        durationSeconds: Int,
        coverImageUrl: String? = null,
        coverVideoUrl: String? = null,
        backgroundHint: String? = null
    ): PlaybackManifest {
        val segments = splitScriptIntoSegments(script, durationSeconds, audioUrl)
        return PlaybackManifest(
            storyId = "story_$storyId",
            title = title,
            storySource = storySource,
            audioUrl = audioUrl,
            coverImageUrl = coverImageUrl,
            coverVideoUrl = coverVideoUrl,
            scenes = listOf(
                PlaybackScene(
                    sceneId = "scene_1",
                    backgroundHint = backgroundHint,
                    segments = segments
                )
            ),
            totalDurationMs = durationSeconds * 1000L
        )
    }

    private fun splitScriptIntoSegments(script: String, totalDurationSeconds: Int, audioUrl: String): List<PlaybackSegment> {
        val paragraphs = script.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) {
            val ms = totalDurationSeconds * 1000L
            return listOf(PlaybackSegment("seg_1", "Narrator", script.takeIf { it.isNotBlank() } ?: "...", audioUrl, ms))
        }
        val msPerSegment = if (paragraphs.size > 1) (totalDurationSeconds * 1000L) / paragraphs.size else totalDurationSeconds * 1000L
        return paragraphs.mapIndexed { i, text ->
            PlaybackSegment(
                segmentId = "seg_${i + 1}",
                speaker = "Narrator",
                text = text,
                audioUrl = audioUrl,
                durationMs = msPerSegment
            )
        }
    }
}

/**
 * Playback manifest per Tamixa spec: storyId ("story_123"), title, scenes with segments.
 * App streams audioUrl and displays subtitle text per segment.
 * coverImageUrl/coverVideoUrl for visual display during playback.
 */
data class PlaybackManifest(
    val storyId: String,
    val title: String,
    val storySource: String,
    val audioUrl: String,
    val coverImageUrl: String? = null,
    val coverVideoUrl: String? = null,
    val scenes: List<PlaybackScene>,
    val totalDurationMs: Long
)

data class PlaybackScene(
    val sceneId: String,
    val backgroundHint: String?,
    val segments: List<PlaybackSegment>
)

data class PlaybackSegment(
    val segmentId: String,
    val speaker: String,
    val text: String,
    val audioUrl: String,
    val durationMs: Long
)
