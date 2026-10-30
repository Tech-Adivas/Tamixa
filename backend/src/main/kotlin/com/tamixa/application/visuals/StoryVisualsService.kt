package com.tamixa.application.visuals

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.narration.NarrationWordTimingService
import com.tamixa.application.port.ImageGenerationPort
import com.tamixa.application.port.ImageStoragePort
import com.tamixa.application.port.ProxyStoragePort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import com.tamixa.application.port.visuals.StoryVisualsRepositoryPort
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.application.stream.StreamLanguageUtils
import com.tamixa.domain.visuals.StoryVisualImage
import com.tamixa.domain.visuals.StoryVisualImageStatus
import com.tamixa.domain.visuals.StoryVisualSet
import com.tamixa.domain.visuals.StoryVisualSetStatus
import com.tamixa.domain.visuals.StoryVisualShot
import com.tamixa.infrastructure.config.AppProperties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.roundToInt

/**
 * Story slideshow: plans pictures and camera shots for a library story, generates the pictures in the background,
 * and turns the shots into timed scenes for playback.
 *
 * Generation is started by an admin, runs once per story (pictures are shared by every language and voice) and is
 * shown to children only after an admin approves it. Shot times are computed per language and voice from that
 * narration's word timings, so they stay exact for cloned voices and translations.
 */
@Service
class StoryVisualsService(
    private val repository: StoryVisualsRepositoryPort,
    private val planner: StoryVisualPlanner,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val wordTimingService: NarrationWordTimingService,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
    @Autowired(required = false) private val imageGeneration: ImageGenerationPort?,
    @Autowired(required = false) private val imageStorage: ImageStoragePort?,
    @Autowired(required = false) private val proxyStorage: ProxyStoragePort?,
    @Value("\${app.story-visuals.enabled:true}") private val enabled: Boolean = true,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    private val jobExecutor = Executors.newFixedThreadPool(2) { r -> Thread(r, "story-visuals-job").apply { isDaemon = true } }
    private val imageExecutor = Executors.newFixedThreadPool(3) { r -> Thread(r, "story-visuals-image").apply { isDaemon = true } }

    @PreDestroy
    fun shutdown() {
        jobExecutor.shutdownNow()
        imageExecutor.shutdownNow()
    }

    fun isRunning(storyId: Long): Boolean = storyId in running

    /** Plans the slideshow and generates every picture in the background. Replaces any earlier slideshow. */
    fun startGeneration(storyId: Long): StoryVisualSet {
        check(enabled) { "Story slideshows are disabled (app.story-visuals.enabled=false)" }
        checkNotNull(imageGeneration) { "No image generation provider is configured" }
        val source = sourceText(storyId)
        check(running.add(storyId)) { "A slideshow is already being generated for this story" }
        val existing = repository.findSetByStoryId(storyId)
        val set = repository.saveSet(
            StoryVisualSet(
                id = existing?.id ?: 0,
                storyId = storyId,
                status = StoryVisualSetStatus.PLANNING,
                sourceLanguage = source.language,
                createdAt = existing?.createdAt ?: Instant.now(),
            )
        )
        log.info("Story visuals generation started storyId={} lang={} words={}", storyId, source.language, source.text.split(Regex("\\s+")).size)
        submit(storyId) { generateAll(set, source) }
        return set
    }

    /** Redraws one picture (optionally with an edited prompt) in the background. */
    fun regenerateImage(storyId: Long, imageKey: String, promptOverride: String?): StoryVisualImage {
        checkNotNull(imageGeneration) { "No image generation provider is configured" }
        val set = repository.findSetByStoryId(storyId) ?: throw NoSuchElementException("No slideshow for story $storyId")
        val image = repository.findImages(set.id).firstOrNull { it.imageKey == imageKey }
            ?: throw NoSuchElementException("No picture $imageKey in story $storyId")
        check(running.add(storyId)) { "This story's slideshow is busy; try again when it has finished" }
        val pending = repository.saveImage(
            image.copy(prompt = promptOverride?.trim()?.takeIf { it.length >= 20 } ?: image.prompt, status = StoryVisualImageStatus.PENDING, lastError = null)
        )
        submit(storyId) {
            val references = references(set)
            drawImage(set, pending, references)
            refreshSetStatus(set.storyId)
        }
        return pending
    }

    fun approve(storyId: Long): StoryVisualSet {
        val set = repository.findSetByStoryId(storyId) ?: throw NoSuchElementException("No slideshow for story $storyId")
        check(set.status == StoryVisualSetStatus.READY) { "Every picture must be ready before approving" }
        return repository.saveSet(set.copy(approvedAt = Instant.now()))
    }

    fun unapprove(storyId: Long): StoryVisualSet {
        val set = repository.findSetByStoryId(storyId) ?: throw NoSuchElementException("No slideshow for story $storyId")
        return repository.saveSet(set.copy(approvedAt = null))
    }

    fun delete(storyId: Long) {
        check(storyId !in running) { "This story's slideshow is busy; try again when it has finished" }
        repository.deleteSetByStoryId(storyId)
        log.info("Story visuals deleted storyId={}", storyId)
    }

    fun adminView(storyId: Long): StoryVisualsView? {
        val set = repository.findSetByStoryId(storyId) ?: return null
        val images = repository.findImages(set.id)
        val shots = repository.findShots(set.id)
        return StoryVisualsView(
            set = set,
            running = storyId in running,
            characterSheetUrl = resolveImageUrl(set.characterSheetPath),
            images = images.map { it to resolveImageUrl(it.imagePath) },
            shots = shots,
        )
    }

    /**
     * Timed shots for playback of [language]/[voiceProfile] (the narration served at [servedUrl]), or null when the
     * story has no approved slideshow. Each shot starts where its first word is spoken in that narration.
     */
    fun timedShots(storyId: Long, language: String, voiceProfile: String, servedUrl: String, durationSeconds: Int?): List<TimedShot>? {
        return try {
            val set = repository.findSetByStoryId(storyId)?.takeIf { it.visibleToChildren } ?: return null
            val images = repository.findImages(set.id).associateBy { it.imageKey }
            val shots = repository.findShots(set.id).filter { images[it.imageKey]?.imagePath != null }
            if (shots.isEmpty()) return null
            val timings = wordTimingService.getLibraryWordTimings(storyId, language, voiceProfile, servedUrl)
            val duration = durationSeconds?.takeIf { it > 0 }?.toDouble() ?: timings?.lastOrNull()?.endSec
            val sameLanguage = StreamLanguageUtils.normalize(language) == set.sourceLanguage
            val sourceWords = set.sourceWordCount.coerceAtLeast(1)
            var lastProgress = 0f
            shots.mapIndexed { i, shot ->
                val fraction = shot.startWord / sourceWords.toDouble()
                val progress = if (i == 0) {
                    0f
                } else if (!timings.isNullOrEmpty() && duration != null && duration > 0) {
                    // Same text: the shot's own word. A translation: the word at the same relative position.
                    val word = if (sameLanguage) shot.startWord else (fraction * timings.size).roundToInt()
                    (timings[word.coerceIn(0, timings.lastIndex)].startSec / duration).toFloat()
                } else {
                    fraction.toFloat()
                }.coerceIn(lastProgress, 1f)
                lastProgress = progress
                TimedShot(
                    index = i,
                    startProgress = progress,
                    imageUrl = resolveImageUrl(images.getValue(shot.imageKey).imagePath)!!,
                    camera = shot.camera,
                    focus = shot.focus,
                    movement = shot.movement,
                )
            }
        } catch (e: Exception) {
            log.warn("Story visuals timing failed storyId={} lang={} voice={}: {}", storyId, language, voiceProfile, e.message)
            null
        }
    }

    private fun generateAll(initial: StoryVisualSet, source: SourceText) {
        var set = initial
        try {
            val plan = planner.plan(source.text, source.language, source.durationSeconds)
            set = repository.saveSet(
                set.copy(
                    status = StoryVisualSetStatus.GENERATING,
                    sourceWordCount = plan.sourceWordCount,
                    charactersJson = plan.charactersJson,
                    characterSheetPath = null,
                    lastError = null,
                    approvedAt = null,
                )
            )
            repository.replacePlan(
                set.id,
                plan.images.map {
                    StoryVisualImage(
                        setId = set.id,
                        imageKey = it.imageKey,
                        storyMoment = it.storyMoment,
                        prompt = it.prompt,
                        subjectsJson = objectMapper.writeValueAsString(it.subjects),
                    )
                },
                plan.shots.mapIndexed { i, s ->
                    StoryVisualShot(
                        setId = set.id,
                        shotIndex = i,
                        imageKey = s.imageKey,
                        narrationStart = s.narrationStart,
                        startWord = s.startWord,
                        camera = s.camera,
                        focus = s.focus,
                        movement = s.movement,
                    )
                },
            )
            val cover = coverBytes(set.storyId)
            val sheet = imageGeneration!!.generateStoryScene(characterSheetPrompt(plan.charactersJson), listOfNotNull(cover))
                ?.let { storeJpeg(set.storyId, "character_sheet", it) }
            if (sheet == null) log.warn("Story visuals: character sheet failed storyId={}; pictures use the cover only", set.storyId)
            set = repository.saveSet(set.copy(characterSheetPath = sheet))
            val references = references(set)
            repository.findImages(set.id)
                .map { image -> imageExecutor.submit(Callable { drawImage(set, image, references) }) }
                .forEach { it.get() }
            refreshSetStatus(set.storyId)
        } catch (e: Exception) {
            log.warn("Story visuals generation failed storyId={}: {}", set.storyId, e.message)
            repository.saveSet(set.copy(status = StoryVisualSetStatus.FAILED, lastError = e.message ?: e.javaClass.simpleName))
        }
    }

    private fun drawImage(set: StoryVisualSet, image: StoryVisualImage, references: List<ByteArray>) {
        val names = subjectNames(image)
        // Without this the model tends to copy the sheet's several poses into the scene (the same child drawn 3-6 times).
        val cast = if (names.isEmpty()) "" else
            " This picture shows exactly: ${names.joinToString(", ") { "$it (once)" }}. Each character appears only ONCE;" +
                " the reference sheet shows the same person in several outfits, never draw a character more than once" +
                " and do not copy the sheet's layout."
        val prompt = "${image.prompt}$cast Draw every character exactly as shown on the character reference sheet" +
            " (first reference image), in the same soft painterly storybook style as the references."
        val bytes = imageGeneration!!.generateStoryScene(prompt, references)
        val path = bytes?.let { storeJpeg(set.storyId, "${image.imageKey}_${System.currentTimeMillis()}", it) }
        repository.saveImage(
            if (path != null) image.copy(imagePath = path, status = StoryVisualImageStatus.READY, lastError = null)
            else image.copy(status = StoryVisualImageStatus.FAILED, lastError = "Image generation or upload failed")
        )
        log.info("Story visuals picture {} storyId={} {}", image.imageKey, set.storyId, if (path != null) "ready" else "FAILED")
    }

    /** READY when every picture is ready; FAILED (with the count) when some are not. */
    private fun refreshSetStatus(storyId: Long) {
        val set = repository.findSetByStoryId(storyId) ?: return
        val images = repository.findImages(set.id)
        val failed = images.count { it.status != StoryVisualImageStatus.READY }
        repository.saveSet(
            if (images.isNotEmpty() && failed == 0) set.copy(status = StoryVisualSetStatus.READY, lastError = null)
            else set.copy(status = StoryVisualSetStatus.FAILED, lastError = "$failed of ${images.size} pictures failed; regenerate them")
        )
    }

    private fun references(set: StoryVisualSet): List<ByteArray> =
        listOfNotNull(set.characterSheetPath?.let { proxyStorage?.getObject(it) }, coverBytes(set.storyId))

    private fun coverBytes(storyId: Long): ByteArray? {
        val key = storyLibraryRepository.findById(storyId)?.coverImageUrl
            ?.takeIf { it.startsWith("curated_covers/") || it.startsWith("covers/") } ?: return null
        return proxyStorage?.getObject(key)
    }

    private fun characterSheetPrompt(charactersJson: String): String =
        "Character reference sheet for a children's storybook, on a plain soft cream background, in the same soft " +
            "painterly art style as the reference image. A simple lineup: each character ONCE per listed outfit, " +
            "full-body front view, standing apart from the others; each animal or fish once, from the side. " +
            "No scenery. Characters: $charactersJson"

    private fun subjectNames(image: StoryVisualImage): List<String> = try {
        image.subjectsJson?.let { json -> objectMapper.readTree(json).mapNotNull { it.path("name").asText("").ifBlank { null } } }.orEmpty().distinct()
    } catch (e: Exception) {
        emptyList()
    }

    /** PNG from the image model → JPEG (~10x smaller) for fast loading on phones. */
    private fun storeJpeg(storyId: Long, name: String, imageBytes: ByteArray): String? {
        val jpeg = try {
            val source = ImageIO.read(imageBytes.inputStream()) ?: return null
            val rgb = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
            rgb.createGraphics().apply { drawImage(source, 0, 0, java.awt.Color.WHITE, null); dispose() }
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            val out = ByteArrayOutputStream()
            ImageIO.createImageOutputStream(out).use { stream ->
                writer.output = stream
                val params = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.85f
                }
                writer.write(null, IIOImage(rgb, null, null), params)
                writer.dispose()
            }
            out.toByteArray()
        } catch (e: Exception) {
            log.warn("Story visuals JPEG conversion failed storyId={} name={}: {}", storyId, name, e.message)
            return null
        }
        return imageStorage?.storeStoryVisualImage(storyId, name, jpeg)
    }

    private fun resolveImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val base = appProperties.audio.publicBaseUrl.trimEnd('/')
        return coverImageUrlResolver.resolveCoverPath(path)?.let { if (it.startsWith("http")) it else "$base$it" }
    }

    /** The text the source-language narration was spoken from (narration script, else translation content). */
    private fun sourceText(storyId: Long): SourceText {
        val story = storyLibraryRepository.findById(storyId) ?: throw NoSuchElementException("Story $storyId not found")
        val language = StreamLanguageUtils.normalize(story.language)
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language)
        val spoken = translation?.let { narrationScriptRepository.findByTranslationId(it.id)?.scriptText ?: it.content } ?: story.content
        val text = NarrationTextUtils.stripRemainingMarkers(spoken).trim()
        require(text.isNotBlank()) { "Story $storyId has no text to plan a slideshow from" }
        val duration = translation?.let { narrationAudioRepository.findByTranslationIdAndVoiceProfile(it.id, "default")?.durationSeconds } ?: 0
        return SourceText(text, language, duration)
    }

    private fun submit(storyId: Long, work: () -> Unit) {
        try {
            jobExecutor.execute {
                try {
                    work()
                } catch (e: Exception) {
                    log.warn("Story visuals job failed storyId={}: {}", storyId, e.message)
                } finally {
                    running.remove(storyId)
                }
            }
        } catch (e: Exception) {
            running.remove(storyId)
            throw e
        }
    }

    private data class SourceText(val text: String, val language: String, val durationSeconds: Int)
}

data class StoryVisualsView(
    val set: StoryVisualSet,
    val running: Boolean,
    val characterSheetUrl: String?,
    val images: List<Pair<StoryVisualImage, String?>>,
    val shots: List<StoryVisualShot>,
)

/** A camera shot ready for the app: when it starts (0..1 of the narration) and how to frame its picture. */
data class TimedShot(
    val index: Int,
    val startProgress: Float,
    val imageUrl: String,
    val camera: String,
    val focus: String?,
    val movement: String,
)
