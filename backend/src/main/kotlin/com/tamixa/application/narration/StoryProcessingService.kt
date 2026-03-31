package com.tamixa.application.narration

import com.tamixa.common.JobType
import com.tamixa.application.port.ProcessingJobRepositoryPort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StorySceneRepositoryPort
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import com.tamixa.application.translation.TranslationService
import com.tamixa.application.storylibrary.StoryStatus
import com.tamixa.application.narration.EmotionToToneMapper
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.narration.EmotionValidationException
import com.tamixa.domain.TranslationPipelineStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationScript
import com.tamixa.domain.narration.ToneMode
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.narration.Mp3DurationReader
import com.tamixa.infrastructure.narration.NarrationTruncationPolicy
import com.tamixa.infrastructure.narration.PipelineStatusUpdater
import com.tamixa.infrastructure.observability.NarrationMetrics
import com.tamixa.infrastructure.observability.NarrationPipelineMetrics
import com.tamixa.infrastructure.observability.PipelineProgressTracker
import org.slf4j.MDC
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Production-grade narration pipeline: pre-generate all story audio at publish time.
 * 2-step LLM: Translate → Rewrite conversationally. Neural TTS only. No voice cloning, no HLS.
 * Idempotent per (storyId, language, voice). Retry max 3 times with exponential backoff (2s, 5s, 10s).
 */
@Service
class StoryProcessingService(
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    @Lazy private val storyLibraryService: StoryLibraryService,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val storySceneRepository: StorySceneRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    private val translationService: TranslationService,
    private val rewriteService: RewriteService,
    private val emotionTaggingService: EmotionTaggingService,
    private val ssmlBuilder: SSMLBuilderService,
    private val ttsService: TTSService,
    private val audioStorage: AudioStorageService,
    private val appProperties: AppProperties,
    private val pipelineMetrics: NarrationPipelineMetrics,
    private val narrationMetrics: NarrationMetrics,
    private val progressTracker: PipelineProgressTracker,
    private val pipelineStatusUpdater: PipelineStatusUpdater,
    @Qualifier("pipelineLanguageExecutor") private val pipelineLanguageExecutor: ExecutorService,
    @Qualifier("pipelineRewriteExecutor") private val pipelineRewriteExecutor: ExecutorService,
    @Qualifier("pipelineTtsExecutor") private val pipelineTtsExecutor: ExecutorService,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor,
    @Lazy private val self: StoryProcessingService,
    @org.springframework.beans.factory.annotation.Autowired(required = false) private val processingJobRepository: ProcessingJobRepositoryPort? = null
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries = 3
    private val failedPlaceholderContent = "[Pipeline failed]"
    /** Upper bound for translated + rewritten story body (see app.story.max-words, default 900). */
    private val maxPipelineStoryWords: Int
        get() = appProperties.story.maxWords.coerceIn(200, 2500)

    private data class PipelineSourceText(
        val languageCode: String,
        val content: String,
        val title: String?,
        val moral: String?
    )

    /** Use the story's master language row/translation as translation API source (not fixed pipeline default). */
    private fun resolvePipelineSourceText(story: LibraryStory, targetLang: String): PipelineSourceText {
        val masterLang = story.language.trim().lowercase().take(10).ifEmpty { sourceLanguage }
        if (targetLang == masterLang) {
            return PipelineSourceText(masterLang, story.content, story.title, story.moral)
        }
        val tr = translationRepository.findByMasterStoryIdAndLanguage(story.id, masterLang)
        val content = tr?.content?.takeIf { it.isNotBlank() && !it.startsWith(failedPlaceholderContent) }
            ?: story.content
        val title = tr?.title?.takeIf { it.isNotBlank() } ?: story.title
        val moral = tr?.moral?.takeIf { it.isNotBlank() } ?: story.moral
        return PipelineSourceText(masterLang, content, title, moral)
    }

    private data class CuratedTtsOutcome(
        val audioUrl: String,
        val durationSeconds: Int,
        val segmentAudioUrls: List<String>?,
        val truncationWarning: Boolean
    )

    private val terminalReviewStatuses = StoryStatus.TERMINAL_REVIEW

    private fun canPipelineUpdateStatus(masterStoryId: Long): Boolean =
        storyLibraryRepository.findById(masterStoryId)?.status !in terminalReviewStatuses
    private val supportedLanguages: List<String>
        get() = (listOf(sourceLanguage) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()
    private val sourceLanguage: String
        get() = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
    private val defaultVoice = "default"

    /** Languages that receive translate/rewrite/TTS in the curated pipeline for this story. */
    private fun pipelineLanguagesForStory(story: LibraryStory): List<String> {
        if (!appProperties.translationPipeline.masterOnlyNarration) return supportedLanguages
        val m = story.language.trim().lowercase().take(10).ifEmpty { sourceLanguage }
        return listOf(m)
    }

    private fun masterHasPlayableAudio(masterStoryId: Long, story: LibraryStory): Boolean {
        val masterLang = story.language.trim().lowercase().take(10).ifEmpty { sourceLanguage }
        if (!story.audioFileUrl.isNullOrBlank()) return true
        val translation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, masterLang) ?: return false
        return narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
            translation.id, defaultVoice, NarrationAudioStatus.READY
        )
    }

    /** Non-blocking status update via JdbcTemplate (no JPA). Avoids lock contention in pipeline hot path. */
    private fun updatePipelineStatus(translationId: Long, status: TranslationPipelineStatus, lastError: String?) {
        pipelineStatusUpdater.updateStatus(translationId, status, lastError)
    }

    /** Extract the best error message for frontend display, walking cause chain if needed. */
    private fun extractPipelineErrorMessage(e: Exception): String {
        var current: Throwable? = e
        var bestMsg = e.message?.takeIf { it.isNotBlank() }
        while (current != null) {
            val msg = current.message?.takeIf { it.isNotBlank() }
            if (msg != null && (bestMsg == null || msg.length > (bestMsg?.length ?: 0) || msg.contains("rate_limit") || msg.contains("does not exist"))) {
                bestMsg = msg
            }
            current = current.cause
        }
        return bestMsg?.take(500) ?: "Unknown error"
    }

    /** True if this looks like a shutdown-time error (bean destruction, etc). Log and treat as graceful abort. */
    private fun isShutdownRelated(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val msg = current.message ?: ""
            if (msg.contains("in destruction") || msg.contains("BeanFactory") && msg.contains("destroy")) return true
            current = current.cause
        }
        return false
    }

    /** Returns true if language has playable audio (library_stories.audio_file_url for source, else narration READY). */
    private fun languageHasAudio(masterStoryId: Long, lang: String, story: com.tamixa.domain.LibraryStory): Boolean {
        if (appProperties.translationPipeline.masterOnlyNarration) {
            return masterHasPlayableAudio(masterStoryId, story)
        }
        if (lang == sourceLanguage && !story.audioFileUrl.isNullOrBlank()) return true
        val translation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, lang) ?: return false
        return narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
            translation.id, defaultVoice, NarrationAudioStatus.READY
        )
    }

    /**
     * Process only the given languages.
     * @param sequential When true (retry path), process one language at a time to avoid permit contention and timeouts.
     *                   When false (fresh publish), process in parallel for speed.
     * @param forceRegenerate When true (regenerate path), do not skip based on existing READY audio; always run pipeline.
     * @param translationOnly When true, stop after translate + rewrite; persist script and set [TranslationPipelineStatus.AWAITING_AUDIO] (no TTS).
     */
    private fun processLanguagesInternal(
        masterStoryId: Long,
        languagesToProcess: List<String>,
        sequential: Boolean = false,
        forceRegenerate: Boolean = false,
        translationOnly: Boolean = false
    ) {
        if (languagesToProcess.isEmpty()) {
            log.warn("processLanguagesInternal: empty languages list, skipping masterStoryId={}", masterStoryId)
            return
        }
        val story = storyLibraryRepository.findById(masterStoryId)
            ?: run {
                log.warn("Master story {} not found for processing", masterStoryId)
                return
            }
        // Do not run pipeline if story was already sent for changes or rejected
        if (story.status in terminalReviewStatuses) {
            log.info("PIPELINE >>> masterStoryId={} skipped: status already {}", masterStoryId, story.status)
            return
        }
        storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PROCESSING)
        val now = Instant.now()
        val jobId = processingJobRepository?.save(
            jobType = JobType.STORY_PIPELINE.value,
            resourceType = "curated_story",
            resourceId = masterStoryId.toString(),
            status = "IN_PROGRESS",
            progress = 0,
            startedAt = now,
            finishedAt = null,
            errorMessage = null,
            metadata = """{"languages":${languagesToProcess.size},"sequential":$sequential,"translationOnly":$translationOnly}"""
        )
        log.info(
            "PIPELINE >>> masterStoryId={} languages={} jobId={} sequential={} translationOnly={}",
            masterStoryId, languagesToProcess, jobId, sequential, translationOnly
        )
        val toneMode = EmotionToToneMapper.toToneMode(story.emotionMode)
        try {
            val timeoutMinutes = appProperties.translationPipeline.languageTimeoutMinutes.coerceIn(2, 15)
            if (sequential) {
                for (language in languagesToProcess) {
                    MDC.put("masterStoryId", masterStoryId.toString())
                    MDC.put("language", language)
                    try {
                        val langStart = System.nanoTime()
                        val future = java.util.concurrent.CompletableFuture.runAsync(
                            {
                                self.processLanguage(
                                    masterStoryId, language, story, story.age, toneMode, forceRegenerate, translationOnly
                                )
                            },
                            pipelineLanguageExecutor
                        )
                        future.get(timeoutMinutes.toLong(), TimeUnit.MINUTES)
                        val langMs = (System.nanoTime() - langStart) / 1_000_000
                        log.info("PIPELINE >>> masterStoryId={} lang={} DONE {}ms", masterStoryId, language, langMs)
                    } catch (e: TimeoutException) {
                        log.warn("PIPELINE >>> masterStoryId={} lang={} TIMEOUT {}min", masterStoryId, language, timeoutMinutes)
                        translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let { t ->
                            pipelineStatusUpdater.updateStatus(t.id, TranslationPipelineStatus.TTS_FAILED, "Timeout after ${timeoutMinutes}min")
                        }
                    } finally {
                        MDC.remove("language")
                    }
                }
            } else {
                val tasks = languagesToProcess.map { language ->
                    Callable {
                        MDC.put("masterStoryId", masterStoryId.toString())
                        MDC.put("language", language)
                        try {
                            val langStart = System.nanoTime()
                            self.processLanguage(
                                masterStoryId, language, story, story.age, toneMode, forceRegenerate, translationOnly
                            )
                            val langMs = (System.nanoTime() - langStart) / 1_000_000
                            log.info("PIPELINE >>> masterStoryId={} lang={} DONE {}ms", masterStoryId, language, langMs)
                        } finally {
                            MDC.remove("language")
                        }
                    }
                }
                // Use invokeAll with timeout so one stuck language does not block the whole pipeline indefinitely.
                val totalTimeoutMinutes = appProperties.translationPipeline.totalTimeoutMinutes.coerceIn(timeoutMinutes, 120)
                val futures = pipelineLanguageExecutor.invokeAll(tasks, totalTimeoutMinutes.toLong(), TimeUnit.MINUTES)
                futures.forEachIndexed { i, future ->
                    val language = languagesToProcess[i]
                    try {
                        if (future.isCancelled) {
                            log.warn("PIPELINE >>> masterStoryId={} lang={} CANCELLED (total timeout {}min)", masterStoryId, language, totalTimeoutMinutes)
                            translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let { t ->
                                pipelineStatusUpdater.updateStatus(t.id, TranslationPipelineStatus.TTS_FAILED, "Cancelled after total timeout ${totalTimeoutMinutes}min")
                            }
                        } else {
                            future.get(1, TimeUnit.SECONDS) // Already done; short get to retrieve result or propagate exception
                        }
                    } catch (e: CancellationException) {
                        log.warn("PIPELINE >>> masterStoryId={} lang={} CANCELLED", masterStoryId, language)
                        translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let { t ->
                            pipelineStatusUpdater.updateStatus(t.id, TranslationPipelineStatus.TTS_FAILED, "Cancelled (timeout)")
                        }
                    } catch (e: ExecutionException) {
                        val cause = e.cause?.message?.take(200)
                        log.warn("PIPELINE >>> masterStoryId={} lang={} FAILED: {}", masterStoryId, language, cause ?: e.message)
                        translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let { t ->
                            pipelineStatusUpdater.updateStatus(t.id, TranslationPipelineStatus.TTS_FAILED, cause)
                        }
                    } catch (e: TimeoutException) {
                        future.cancel(true)
                        log.warn("PIPELINE >>> masterStoryId={} lang={} TIMEOUT", masterStoryId, language)
                        translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let { t ->
                            pipelineStatusUpdater.updateStatus(t.id, TranslationPipelineStatus.TTS_FAILED, "Timeout")
                        }
                    }
                }
            }

            val storyNow = storyLibraryRepository.findById(masterStoryId)!!
            // Do not overwrite status if admin already sent for changes or rejected while pipeline was running
            if (storyNow.status in terminalReviewStatuses) {
                log.info("PIPELINE >>> masterStoryId={} completed but status already {} (admin acted); leaving status unchanged", masterStoryId, storyNow.status)
                jobId?.let { processingJobRepository?.updateStatus(it, "CANCELLED", 0, Instant.now(), "Story sent for changes or rejected during pipeline") }
                return
            }
            val pipelineLangs = pipelineLanguagesForStory(storyNow)
            val allLangsHaveAudio = pipelineLangs.all { languageHasAudio(masterStoryId, it, storyNow) }
            if (allLangsHaveAudio) {
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.READY)
                pipelineMetrics.recordDailyGeneration()
                jobId?.let { processingJobRepository?.updateStatus(it, "COMPLETED", 100, Instant.now(), null) }
                log.info("PIPELINE >>> COMPLETE masterStoryId={} READY (all {} languages)", masterStoryId, pipelineLangs.size)
            } else {
                val translationsAfter = translationRepository.findByMasterStoryId(masterStoryId)
                val hasFailed = translationsAfter.any { it.status.isFailed() }
                if (translationOnly && !hasFailed) {
                    val allScriptsReady = pipelineLangs.all { lang ->
                        val t = translationsAfter.find { it.language.equals(lang, ignoreCase = true) }
                        t != null &&
                            t.status == TranslationPipelineStatus.AWAITING_AUDIO &&
                            t.content.isNotBlank() &&
                            !t.content.startsWith(failedPlaceholderContent)
                    }
                    if (allScriptsReady) {
                        storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
                        jobId?.let { processingJobRepository?.updateStatus(it, "COMPLETED", 100, Instant.now(), null) }
                        log.info(
                            "PIPELINE >>> TRANSLATION-ONLY COMPLETE masterStoryId={} ({} languages, awaiting approval for TTS)",
                            masterStoryId,
                            pipelineLangs.size
                        )
                    } else {
                        val statusesStr = translationsAfter.joinToString(",") { "${it.language}=${it.status}" }
                        log.warn("Story masterStoryId={} translation-only incomplete: [{}]", masterStoryId, statusesStr)
                        storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
                        jobId?.let {
                            processingJobRepository?.updateStatus(
                                it, "FAILED", 0, Instant.now(), "Translation-only: not all languages have scripts: $statusesStr"
                            )
                        }
                    }
                } else {
                    val statusesStr = translationsAfter.joinToString(",") { "${it.language}=${it.status}" }
                    log.warn("Story masterStoryId={} not ready: statusByLang=[{}]", masterStoryId, statusesStr)
                    storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
                    jobId?.let {
                        processingJobRepository?.updateStatus(
                            it, "FAILED", 0, Instant.now(), "Not all languages have audio: $statusesStr"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            jobId?.let { processingJobRepository?.updateStatus(it, "FAILED", 0, Instant.now(), extractPipelineErrorMessage(e)) }
            throw e
        }
    }

    /**
     * Run pipeline synchronously in caller thread. Use when async does not run (e.g. story stuck at PENDING).
     * Blocks until complete; use for debugging or manual recovery.
     *
     * **No class-level @Transactional here:** each [processLanguage] uses REQUIRES_NEW. A long outer transaction
     * would hold a row lock on `library_stories` after [processLanguagesInternal] sets PROCESSING, blocking admin
     * saves (PUT story) until the pipeline finishes — matching 120s client timeouts on draft save / submit.
     */
    fun processSync(masterStoryId: Long, translationOnly: Boolean = false) {
        if (!tryClaimOrRecoverStale(masterStoryId)) {
            log.info("PIPELINE >>> SKIP masterStoryId={} (already running)", masterStoryId)
            return
        }
        log.info("PIPELINE >>> START masterStoryId={} (processSync translationOnly={})", masterStoryId, translationOnly)
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            val story = storyLibraryRepository.findById(masterStoryId)
            if (story == null) {
                log.warn("PIPELINE >>> SKIP masterStoryId={} (story not found)", masterStoryId)
                return
            }
            val translationsByLang = translationRepository.findByMasterStoryId(masterStoryId).associateBy { it.language }
            val languagesToProcess = pipelineLanguagesForStory(story).filter { lang ->
                if (translationOnly) {
                    val t = translationsByLang[lang]
                    when {
                        t == null -> true
                        t.content.isBlank() || t.content.startsWith(failedPlaceholderContent) -> true
                        t.status == TranslationPipelineStatus.AWAITING_AUDIO -> false
                        t.status.isFailed() -> t.retryCount < maxRetries
                        else -> t.status.canRetry() && t.retryCount < maxRetries
                    }
                } else {
                val hasAudio = languageHasAudio(masterStoryId, lang, story)
                if (!hasAudio) return@filter true
                val translation = translationsByLang[lang] ?: return@filter false
                translation.status.canRetry() && translation.retryCount < maxRetries
                }
            }
            if (languagesToProcess.isEmpty()) {
                val allLangsHaveAudio = pipelineLanguagesForStory(story).all { languageHasAudio(masterStoryId, it, story) }
                if (allLangsHaveAudio && canPipelineUpdateStatus(masterStoryId)) {
                    storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.READY)
                }
                log.info("PIPELINE >>> SKIP masterStoryId={} (no changed/missing languages to process)", masterStoryId)
                return
            }
            log.info("PIPELINE >>> masterStoryId={} selectedLanguages={}", masterStoryId, languagesToProcess)
            processLanguagesInternal(masterStoryId, languagesToProcess, translationOnly = translationOnly)
        } catch (e: Exception) {
            if (isShutdownRelated(e)) {
                log.warn("PIPELINE >>> ABORTED masterStoryId={} (application shutting down)", masterStoryId)
            } else {
                log.error("PIPELINE >>> FAILED masterStoryId={} error={}", masterStoryId, e.message, e)
            }
            pipelineMetrics.recordNarrationFailure()
            if (canPipelineUpdateStatus(masterStoryId)) {
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
            }
        } finally {
            progressTracker.clearProcessing(masterStoryId)
            MDC.remove("masterStoryId")
        }
    }

    /** Process only the given languages. Used for retry and republish. */
    @Async
    fun processLanguagesAsync(masterStoryId: Long, languagesToProcess: List<String>) {
        MDC.put("masterStoryId", masterStoryId.toString())
        val start = System.nanoTime()
        try {
            processLanguagesInternal(masterStoryId, languagesToProcess)
        } catch (e: Exception) {
            if (isShutdownRelated(e)) {
                log.warn("Pipeline aborted for masterStoryId={} (application shutting down)", masterStoryId)
            } else {
                log.error("Pipeline failed for masterStoryId={} error={} cause={}", masterStoryId, e.message, e.cause?.message, e)
            }
            pipelineMetrics.recordNarrationFailure()
            if (canPipelineUpdateStatus(masterStoryId)) {
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
            }
        } finally {
            progressTracker.clearProcessing(masterStoryId)
            pipelineMetrics.recordNarrationLatency(System.nanoTime() - start)
            MDC.remove("masterStoryId")
        }
    }

    /**
     * Trigger async processing when admin publishes story.
     * Sets status to PROCESSING, processes all languages, marks READY when all COMPLETED.
     *
     * **No @Transactional on this method** — see [processSync] kdoc (admin PUT must not block on library row lock).
     */
    @Async
    fun processAsync(masterStoryId: Long) {
        log.info("processAsync ASYNC START masterStoryId={}", masterStoryId)
        MDC.put("masterStoryId", masterStoryId.toString())
        val start = System.nanoTime()
        try {
            val story = storyLibraryRepository.findById(masterStoryId)
                ?: run {
                    log.warn("Master story {} not found for processing", masterStoryId)
                    return
                }
            if (story.status in terminalReviewStatuses) {
                log.info("PIPELINE >>> masterStoryId={} (processAsync) skipped: status already {}", masterStoryId, story.status)
                return
            }
            storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PROCESSING)
            val languages = pipelineLanguagesForStory(story)
            log.info("Pipeline START masterStoryId={} languages={} (parallel)", masterStoryId, languages)
            val toneMode = EmotionToToneMapper.toToneMode(story.emotionMode)
            val timeoutMinutes = appProperties.translationPipeline.languageTimeoutMinutes.coerceIn(2, 15)
            val tasks = languages.map { language ->
                Callable {
                    MDC.put("masterStoryId", masterStoryId.toString())
                    MDC.put("language", language)
                    try {
                        val langStart = System.nanoTime()
                        self.processLanguage(masterStoryId, language, story, story.age, toneMode)
                        val langMs = (System.nanoTime() - langStart) / 1_000_000
                        log.info("PIPELINE >>> masterStoryId={} lang={} DONE {}ms", masterStoryId, language, langMs)
                    } finally {
                        MDC.remove("language")
                    }
                }
            }
            val futures = pipelineLanguageExecutor.invokeAll(tasks)
            futures.forEachIndexed { i, future ->
                val language = languages[i]
                try {
                    future.get(timeoutMinutes.toLong(), TimeUnit.MINUTES)
                } catch (e: java.util.concurrent.TimeoutException) {
                    future.cancel(true)
                    log.warn("PIPELINE masterStoryId={} lang={} timed out after {}min — marking failed, other languages continue", masterStoryId, language, timeoutMinutes)
                    translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let { t ->
                        pipelineStatusUpdater.updateStatus(t.id, TranslationPipelineStatus.TTS_FAILED, "Timeout after ${timeoutMinutes}min")
                    }
                } catch (e: Exception) {
                    log.warn("PIPELINE >>> masterStoryId={} lang={} FAILED: {}", masterStoryId, language, e.message)
                }
            }

            val storyNow = storyLibraryRepository.findById(masterStoryId)!!
            if (storyNow.status in terminalReviewStatuses) {
                log.info("PIPELINE >>> masterStoryId={} (processAsync) completed but status already {}; leaving unchanged", masterStoryId, storyNow.status)
                return
            }
            val pipelineLangsAsync = pipelineLanguagesForStory(storyNow)
            val allLangsHaveAudio = pipelineLangsAsync.all { languageHasAudio(masterStoryId, it, storyNow) }
            if (allLangsHaveAudio) {
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.READY)
                pipelineMetrics.recordDailyGeneration()
                val totalMs = (System.nanoTime() - start) / 1_000_000
                log.info("Story masterStoryId={} READY: all {} languages have audio in {}ms (~{} min)", masterStoryId, pipelineLangsAsync.size, totalMs, totalMs / 60000)
            } else {
                val missingLangs = pipelineLangsAsync.filter { !languageHasAudio(masterStoryId, it, storyNow) }
                val allTranslations = translationRepository.findByMasterStoryId(masterStoryId)
                val statusesStr = allTranslations.joinToString(",") { "${it.language}=${it.status}" }
                log.warn("Story masterStoryId={} not ready: missing languages {} statusByLang=[{}]", masterStoryId, missingLangs, statusesStr)
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
            }
        } catch (e: Exception) {
            log.error("Story processing failed for masterStoryId={} error={} cause={}", masterStoryId, e.message, e.cause?.message, e)
            pipelineMetrics.recordNarrationFailure()
            if (canPipelineUpdateStatus(masterStoryId)) {
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
            }
        } finally {
            progressTracker.clearProcessing(masterStoryId)
            pipelineMetrics.recordNarrationLatency(System.nanoTime() - start)
            MDC.remove("masterStoryId")
        }
    }

    /** Run exists check in a new transaction so it sees committed deletes (e.g. after invalidate in regenerate). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun existsReadyNarrationAudio(translationId: Long): Boolean =
        narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
            translationId, defaultVoice, NarrationAudioStatus.READY
        )

    /**
     * Process one language: translate, rewrite, optional TTS, scenes.
     *
     * **No @Transactional:** a single long transaction would keep locks on `story_translations` (e.g. after
     * setting [TranslationPipelineStatus.TTS_PROCESSING]) for the entire LLM/TTS window. Admin
     * [com.tamixa.application.storylibrary.StoryLibraryService.update] then blocks on those rows after
     * updating `library_stories`, producing multi‑minute waits and client timeouts (Generate translations /
     * save draft). Repository `save` calls each run in short Spring Data transactions.
     */
    fun processLanguage(
        masterStoryId: Long,
        language: String,
        story: LibraryStory,
        age: Int,
        toneMode: ToneMode = ToneMode.CALM,
        forceRegenerate: Boolean = false,
        translationOnly: Boolean = false
    ) {
        log.info(
            "processLanguage START masterStoryId={} lang={} forceRegenerate={} translationOnly={}",
            masterStoryId, language, forceRegenerate, translationOnly
        )
        val pipelineSource = resolvePipelineSourceText(story, language)
        val translation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)

        if (translation != null && !forceRegenerate) {
            val hasReadyAudio = self.existsReadyNarrationAudio(translation.id)
            if (hasReadyAudio && translation.content.isNotBlank() && !translation.content.startsWith(failedPlaceholderContent)) {
                // Status can be stale (e.g. left in TTS_PROCESSING after retries). Treat READY audio as authoritative.
                if (translation.status != TranslationPipelineStatus.COMPLETED) {
                    translationRepository.save(
                        translation.copy(
                            status = TranslationPipelineStatus.COMPLETED,
                            lastError = null
                        )
                    )
                }
                log.debug("Idempotent skip: READY audio already exists for masterStoryId={} lang={}", masterStoryId, language)
                return
            }
        }

        var currentTranslation = translation
        var retryCount = translation?.retryCount ?: 0
        if (retryCount >= maxRetries) {
            log.warn("Max retries reached for masterStoryId={} lang={}", masterStoryId, language)
            return
        }

        // Mark this language as in progress immediately so pipeline-status API shows "Generating X audio…"
        progressTracker.setProcessing(masterStoryId, language)
        // Only update to TRANSLATING when we don't already have content (avoids redundant UPDATE and potential lock contention)
        val hasContent = currentTranslation?.content?.isNotBlank() == true && currentTranslation?.content?.startsWith(failedPlaceholderContent) != true
        if (currentTranslation != null && !hasContent) {
            updatePipelineStatus(currentTranslation.id, TranslationPipelineStatus.TRANSLATING, null)
        }

        for (attempt in 1..(maxRetries - retryCount)) {
            try {
                var t1 = System.currentTimeMillis()
                val scriptText: String
                val ttsOnlyFromSavedScript =
                    currentTranslation != null && !translationOnly &&
                        (forceRegenerate || currentTranslation.status == TranslationPipelineStatus.AWAITING_AUDIO)
                if (ttsOnlyFromSavedScript) {
                    // TTS only: regeneration or post–content-approval pass. Use narration_script or translation.content.
                    val existingScript = narrationScriptRepository.findByTranslationId(currentTranslation.id)?.scriptText?.takeIf { it.isNotBlank() }
                    val text = existingScript ?: currentTranslation.content?.takeIf { it.isNotBlank() }
                    if (text.isNullOrBlank()) {
                        log.warn(
                            "TTS-only masterStoryId={} lang={}: no existing script or content, skipping. " +
                                "Has narration_script={}, translation.content blank={}",
                            masterStoryId, language,
                            narrationScriptRepository.findByTranslationId(currentTranslation.id) != null,
                            currentTranslation.content.isNullOrBlank()
                        )
                        pipelineStatusUpdater.updateStatus(currentTranslation.id, TranslationPipelineStatus.TTS_FAILED, "No script or content for regeneration")
                        continue
                    }
                    scriptText = StoryPipelineWordLimit.clampToMaxWords(text, maxPipelineStoryWords)
                    log.info(
                        "PIPELINE >>> masterStoryId={} lang={} TTS_ONLY (forceRegenerate={} awaitingAudio={})",
                        masterStoryId, language, forceRegenerate, currentTranslation.status == TranslationPipelineStatus.AWAITING_AUDIO
                    )
                    currentTranslation = translationRepository.save(
                        currentTranslation.copy(status = TranslationPipelineStatus.TTS_PROCESSING, lastError = null)
                    )
                } else {
                    // Generate: translate -> rewrite -> (optional TTS)
                    log.info(
                        "PIPELINE >>> masterStoryId={} lang={} TRANSLATE_START (sourceLang={})",
                        masterStoryId, language, pipelineSource.languageCode
                    )
                    val contentToProcess = runTranslationStep(
                        masterStoryId = masterStoryId,
                        language = language,
                        translationSourceLang = pipelineSource.languageCode,
                        sourceContent = pipelineSource.content,
                        sourceTitle = pipelineSource.title,
                        sourceMoral = pipelineSource.moral,
                        existing = currentTranslation
                    ) ?: continue
                    log.info("PIPELINE >>> masterStoryId={} lang={} TRANSLATE_DONE", masterStoryId, language)
                    val t0 = System.currentTimeMillis()
                    currentTranslation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)!!
                    progressTracker.setProcessing(masterStoryId, language)
                    val waitMs = System.currentTimeMillis() - t0
                    if (waitMs > 5000) log.warn("PIPELINE >>> masterStoryId={} lang={} blocked {}ms between TRANSLATE_DONE and REWRITE_START", masterStoryId, language, waitMs)
                    log.info("PIPELINE >>> masterStoryId={} lang={} REWRITE_START", masterStoryId, language)
                    scriptText = StoryPipelineWordLimit.clampToMaxWords(
                        runRewriteStep(currentTranslation, age, toneMode) ?: continue,
                        maxPipelineStoryWords
                    )
                    log.info("PIPELINE >>> masterStoryId={} lang={} REWRITE_DONE", masterStoryId, language)

                    if (translationOnly) {
                        currentTranslation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)!!
                        val scriptWordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                        val scriptReadingTime = (scriptWordCount / 150.0).coerceIn(0.5, 30.0)
                        val existingScriptRow = narrationScriptRepository.findByTranslationId(currentTranslation.id)
                        narrationScriptRepository.save(
                            StoryNarrationScript(
                                id = existingScriptRow?.id ?: 0,
                                translationId = currentTranslation.id,
                                toneMode = toneMode,
                                scriptText = scriptText,
                                safetyScore = 100,
                                createdAt = existingScriptRow?.createdAt ?: Instant.now()
                            )
                        )
                        translationRepository.save(
                            currentTranslation.copy(
                                content = scriptText,
                                wordCount = scriptWordCount,
                                readingTimeMinutes = scriptReadingTime,
                                status = TranslationPipelineStatus.AWAITING_AUDIO,
                                lastError = null
                            )
                        )
                        log.info(
                            "PIPELINE >>> masterStoryId={} lang={} TRANSLATION-ONLY DONE (script saved, no TTS yet)",
                            masterStoryId,
                            language
                        )
                        return
                    }

                    t1 = System.currentTimeMillis()
                    val audioAlreadyReady = narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                        currentTranslation.id, defaultVoice, NarrationAudioStatus.READY
                    )
                    val contentUnchanged = audioAlreadyReady &&
                        currentTranslation.content?.trim().equals(scriptText.trim(), ignoreCase = false)
                    if (audioAlreadyReady && contentUnchanged) {
                        translationRepository.save(
                            currentTranslation.copy(
                                status = TranslationPipelineStatus.COMPLETED,
                                lastError = null
                            )
                        )
                        return
                    }
                    currentTranslation = translationRepository.save(
                        currentTranslation.copy(status = TranslationPipelineStatus.TTS_PROCESSING, lastError = null)
                    )
                }

                val emotionTagged = getEmotionTaggedOrNull(scriptText, language, currentTranslation.id)
                val storyFresh = storyLibraryRepository.findById(masterStoryId)
                val perSegmentTts = appProperties.narration.perSegmentTts
                if (!forceRegenerate) {
                    val preTtsMs = System.currentTimeMillis() - t1
                    if (preTtsMs > 5000) log.warn("PIPELINE >>> masterStoryId={} lang={} took {}ms between REWRITE_DONE and TTS_START", masterStoryId, language, preTtsMs)
                }
                log.info("PIPELINE >>> masterStoryId={} lang={} TTS_START (perSegment={})", masterStoryId, language, perSegmentTts)
                val ttsStartMs = System.currentTimeMillis()
                val timeoutMinutes = appProperties.translationPipeline.languageTimeoutMinutes.coerceIn(2, 15)
                val scriptWordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
                val expectedSpeechSecs = (scriptWordCount / 150.0 * 60).toInt().coerceAtLeast(10)
                val ttsOutcome = if (perSegmentTts) {
                    runPerSegmentTts(
                        masterStoryId, language, defaultVoice, scriptText, emotionTagged,
                        storyFresh?.theme, age, toneMode, timeoutMinutes,
                        scriptWordCount, expectedSpeechSecs
                    )
                } else {
                    val ssml = if (emotionTagged != null) {
                        ssmlBuilder.buildSSMLFromEmotionTagged(emotionTagged, language, age, toneMode)
                    } else {
                        ssmlBuilder.buildSSML(scriptText, language, age, toneMode)
                    }
                    val ttsStartMsInner = System.currentTimeMillis()
                    val future = pipelineTtsExecutor.submit(Callable { ttsService.synthesize(ssml, language, defaultVoice) })
                    val audioBytes = future.get(timeoutMinutes.toLong(), TimeUnit.MINUTES)
                        ?: throw IllegalStateException("TTS returned null")
                    if (audioBytes.isEmpty()) throw IllegalStateException("TTS returned empty")
                    log.info("TTS_PROCESSING masterStoryId={} lang={} synthesized {} bytes in {}ms", masterStoryId, language, audioBytes.size, System.currentTimeMillis() - ttsStartMsInner)
                    val url = audioStorage.uploadNarrationAudio(masterStoryId, language, defaultVoice, audioBytes)
                    val measured = Mp3DurationReader.durationSecondsOrNull(audioBytes)
                    val durationSeconds = measured ?: expectedSpeechSecs
                    val truncationWarning =
                        measured != null && measured < (expectedSpeechSecs * NarrationTruncationPolicy.MIN_AUDIO_VS_EXPECTED_SPEECH_FRACTION)
                    CuratedTtsOutcome(url, durationSeconds, null, truncationWarning)
                }
                val audioUrl = ttsOutcome.audioUrl
                val durationSeconds = ttsOutcome.durationSeconds
                val segmentAudioUrls = ttsOutcome.segmentAudioUrls
                val truncationWarning = ttsOutcome.truncationWarning
                if (truncationWarning) {
                    log.warn(
                        "PIPELINE >>> masterStoryId={} lang={} possible truncation: duration={}s vs expected~{}s speech (perSegment={})",
                        masterStoryId, language, durationSeconds, expectedSpeechSecs, perSegmentTts
                    )
                }
                val existingAudio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(
                    currentTranslation.id, defaultVoice
                )
                val audio = if (existingAudio != null) {
                    com.tamixa.domain.narration.StoryNarrationAudio(
                        id = existingAudio.id,
                        translationId = currentTranslation.id,
                        voiceProfile = defaultVoice,
                        audioUrl = audioUrl,
                        durationSeconds = durationSeconds,
                        status = NarrationAudioStatus.READY,
                        createdAt = Instant.now(),
                        truncationWarning = truncationWarning
                    )
                } else {
                    com.tamixa.domain.narration.StoryNarrationAudio(
                        id = 0,
                        translationId = currentTranslation.id,
                        voiceProfile = defaultVoice,
                        audioUrl = audioUrl,
                        durationSeconds = durationSeconds,
                        status = NarrationAudioStatus.READY,
                        createdAt = Instant.now(),
                        truncationWarning = truncationWarning
                    )
                }
                narrationAudioRepository.save(audio)

                val masterLang = story.language.trim().lowercase().take(10).ifEmpty { sourceLanguage }
                if (language.equals(masterLang, ignoreCase = true)) {
                    storyLibraryRepository.updateAudioUrl(masterStoryId, audioUrl)
                }

                // Persist scenes + segments (Stage 1–3: scene extraction, narration/dialogue, playback metadata)
                storySceneRepository.saveSceneWithSegments(
                    translationId = currentTranslation.id,
                    script = scriptText,
                    totalDurationSeconds = durationSeconds,
                    audioUrl = audioUrl,
                    theme = storyFresh?.theme,
                    emotionTaggedScript = emotionTagged,
                    segmentAudioUrls = segmentAudioUrls
                )

                // Persist narration script so getNarrationScript returns cached (no re-rewrite on manifest fallback)
                val existingScript = narrationScriptRepository.findByTranslationId(currentTranslation.id)
                narrationScriptRepository.save(
                    StoryNarrationScript(
                        id = existingScript?.id ?: 0,
                        translationId = currentTranslation.id,
                        toneMode = toneMode,
                        scriptText = scriptText,
                        safetyScore = 100,
                        createdAt = existingScript?.createdAt ?: Instant.now()
                    )
                )
                // Persist conversational script as story text so displayed content matches narrated audio.
                // Use actual duration when available so readingTimeMinutes matches real playback.
                val scriptReadingTime = (durationSeconds / 60.0).coerceIn(0.5, 30.0)
                    .takeIf { it > 0 } ?: (scriptWordCount / 150.0).coerceIn(0.5, 30.0)
                translationRepository.save(
                    currentTranslation.copy(
                        content = scriptText,
                        wordCount = scriptWordCount,
                        readingTimeMinutes = scriptReadingTime
                    )
                )
                if (language == sourceLanguage) {
                    storyLibraryRepository.updateContent(masterStoryId, scriptText, scriptWordCount, scriptReadingTime)
                }
                translationRepository.save(
                    currentTranslation.copy(
                        status = TranslationPipelineStatus.COMPLETED,
                        lastError = null
                    )
                )
                pipelineMetrics.recordTtsLatency((System.currentTimeMillis() - ttsStartMs) * 1_000_000L)
                return
            } catch (e: Exception) {
                retryCount++
                val errorMsg = extractPipelineErrorMessage(e)
                log.warn("Attempt {} failed for masterStoryId={} lang={}: {}", attempt, masterStoryId, language, errorMsg)
                val failedStatus = when {
                    errorMsg.contains("translate", ignoreCase = true) -> TranslationPipelineStatus.TRANSLATION_FAILED
                    errorMsg.contains("rewrite", ignoreCase = true) -> TranslationPipelineStatus.REWRITE_FAILED
                    else -> TranslationPipelineStatus.TTS_FAILED
                }
                // Persist failure in the same transaction to avoid self-locking the same row via nested tx updates.
                currentTranslation = if (currentTranslation != null) {
                    translationRepository.save(
                        currentTranslation.copy(
                            status = failedStatus,
                            retryCount = retryCount,
                            lastError = errorMsg.take(500)
                        )
                    )
                } else {
                    translationRepository.save(
                        StoryTranslation(
                            id = 0, masterStoryId = masterStoryId, language = language,
                            title = null, content = failedPlaceholderContent, moral = null,
                            wordCount = 0, readingTimeMinutes = 0.0, createdAt = Instant.now(),
                            status = failedStatus, retryCount = retryCount, lastError = errorMsg.take(500)
                        )
                    )
                }
                pipelineMetrics.recordNarrationFailure()
                if (retryCount >= maxRetries) {
                    log.error("PIPELINE >>> masterStoryId={} lang={} FAILED after {} retries",
                        masterStoryId, language, maxRetries)
                    return
                }
                val backoffMs = when (attempt) {
                    1 -> 2000L
                    2 -> 5000L
                    else -> 10000L
                }
                Thread.sleep(backoffMs)
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun persistProcessingFailure(
        currentTranslation: StoryTranslation?,
        masterStoryId: Long,
        language: String,
        failedStatus: TranslationPipelineStatus,
        errorMsg: String,
        retryCount: Int
    ) {
        if (currentTranslation != null) {
            pipelineStatusUpdater.incrementRetryCount(currentTranslation.id, errorMsg)
            pipelineStatusUpdater.updateStatus(currentTranslation.id, failedStatus, errorMsg)
        } else {
            val failedTranslation = StoryTranslation(
                id = 0, masterStoryId = masterStoryId, language = language,
                title = null, content = failedPlaceholderContent, moral = null,
                wordCount = 0, readingTimeMinutes = 0.0, createdAt = Instant.now(),
                status = failedStatus, retryCount = retryCount, lastError = errorMsg.take(500)
            )
            translationRepository.save(failedTranslation)
        }
    }

    /** Get emotion-tagged script when valid; null to fall back to plain SSML. */
    private fun getEmotionTaggedOrNull(
        scriptText: String,
        language: String,
        translationId: Long
    ): com.tamixa.domain.narration.EmotionTaggedScript? = try {
        val emotionTagged = emotionTaggingService.tagEmotions(scriptText, language)
        if (!emotionTagged.validateWordCountTolerance(15)) {
            throw EmotionValidationException("Emotion tagging word count deviation exceeds 15%")
        }
        emotionTagged
    } catch (e: EmotionValidationException) {
        log.debug("Falling back to plain SSML for translationId={}: {}", translationId, e.message)
        null
    }

    private fun runTranslationStep(
        masterStoryId: Long,
        language: String,
        translationSourceLang: String,
        sourceContent: String,
        sourceTitle: String?,
        sourceMoral: String?,
        existing: StoryTranslation?
    ): String? {
        return try {
            val isSameLanguage = language.trim().equals(translationSourceLang.trim(), ignoreCase = true)
            val forceParaphraseForSameLanguageInThisStep =
                isSameLanguage && (existing == null || existing.status == TranslationPipelineStatus.PENDING)

            if (
                existing != null &&
                    existing.content.isNotBlank() &&
                    !existing.content.startsWith(failedPlaceholderContent) &&
                    !forceParaphraseForSameLanguageInThisStep
            ) {
                var existingForProcessing = existing
                // Skip redundant status update when already REWRITING (avoids DB lock contention)
                if (existingForProcessing.status != TranslationPipelineStatus.REWRITING) {
                    existingForProcessing = translationRepository.save(
                        existingForProcessing.copy(
                            status = TranslationPipelineStatus.REWRITING,
                            lastError = null
                        )
                    )
                }
                // Use persisted translation text only; do not call translation API during narration when content exists.
                return existingForProcessing.content
            }
            if (existing != null) {
                updatePipelineStatus(existing.id, TranslationPipelineStatus.TRANSLATING, null)
            }
            val result = pipelineMetrics.recordTranslationLatency {
                translationService.translateIfNeeded(
                    sourceLang = translationSourceLang,
                    targetLang = language,
                    title = sourceTitle,
                    content = sourceContent,
                    moral = sourceMoral,
                    forceParaphraseForSameLanguage = forceParaphraseForSameLanguageInThisStep
                )
            }
            val body = StoryPipelineWordLimit.clampToMaxWords(result.content, maxPipelineStoryWords)
            val wordCount = body.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
            val toSave = if (existing != null) {
                existing.copy(
                    content = body,
                    title = result.title,
                    moral = result.moral,
                    wordCount = wordCount,
                    readingTimeMinutes = readingTimeMinutes,
                    status = TranslationPipelineStatus.REWRITING
                )
            } else {
                StoryTranslation(
                    id = 0,
                    masterStoryId = masterStoryId,
                    language = language,
                    title = result.title,
                    content = body,
                    moral = result.moral,
                    wordCount = wordCount,
                    readingTimeMinutes = readingTimeMinutes,
                    createdAt = Instant.now(),
                    status = TranslationPipelineStatus.REWRITING
                )
            }
            translationRepository.save(toSave)
            body
        } catch (e: Exception) {
            val msg = extractPipelineErrorMessage(e)
            translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let {
                updatePipelineStatus(it.id, TranslationPipelineStatus.TRANSLATION_FAILED, msg)
            }
            throw e
        }
    }

    /**
     * Per-segment TTS: N synthesizes, N uploads. Duration from decoded MP3 length; [CuratedTtsOutcome.truncationWarning]
     * if any segment or total run is materially shorter than script-based expected speech time (~150 wpm).
     */
    private fun runPerSegmentTts(
        masterStoryId: Long,
        language: String,
        defaultVoice: String,
        scriptText: String,
        emotionTagged: com.tamixa.domain.narration.EmotionTaggedScript?,
        theme: String?,
        age: Int,
        toneMode: ToneMode,
        timeoutMinutes: Int,
        scriptWordCount: Int,
        expectedSpeechSecs: Int
    ): CuratedTtsOutcome {
        val segmentInfos = storySceneRepository.computeSegmentInfosForTts(scriptText, theme, emotionTagged)
        if (segmentInfos.isEmpty()) {
            val ssml = if (emotionTagged != null) ssmlBuilder.buildSSMLFromEmotionTagged(emotionTagged, language, age, toneMode)
            else ssmlBuilder.buildSSML(scriptText, language, age, toneMode)
            val audioBytes = pipelineTtsExecutor.submit(Callable { ttsService.synthesize(ssml, language, defaultVoice) })
                .get(timeoutMinutes.toLong(), TimeUnit.MINUTES) ?: throw IllegalStateException("TTS returned null")
            val url = audioStorage.uploadNarrationAudio(masterStoryId, language, defaultVoice, audioBytes)
            val measured = Mp3DurationReader.durationSecondsOrNull(audioBytes)
            val duration = measured ?: expectedSpeechSecs
            val trunc = measured != null && measured < (expectedSpeechSecs * NarrationTruncationPolicy.MIN_AUDIO_VS_EXPECTED_SPEECH_FRACTION)
            return CuratedTtsOutcome(url, duration, null, trunc)
        }
        val segmentUrls = mutableListOf<String>()
        var totalDurationSec = 0
        var anySegmentTruncated = false
        for ((i, info) in segmentInfos.withIndex()) {
            val singleSeg = com.tamixa.domain.narration.EmotionTaggedScript(
                info.text,
                listOf(com.tamixa.domain.narration.EmotionSegment(info.text, info.emotion)),
                info.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            )
            val ssml = ssmlBuilder.buildSSMLFromEmotionTagged(singleSeg, language, age, toneMode)
            val bytes = pipelineTtsExecutor.submit(Callable { ttsService.synthesize(ssml, language, defaultVoice) })
                .get(timeoutMinutes.toLong(), TimeUnit.MINUTES) ?: throw IllegalStateException("TTS returned null for segment $i")
            val segUrl = audioStorage.uploadSegmentAudio(masterStoryId, language, defaultVoice, i, bytes)
            segmentUrls.add(segUrl)
            val measured = Mp3DurationReader.durationSecondsOrNull(bytes)
            val segWords = info.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
            val segExpected =
                (segWords.toDouble() / scriptWordCount.toDouble() * expectedSpeechSecs).toInt().coerceAtLeast(2)
            val segDur = measured ?: segExpected
            totalDurationSec += segDur
            if (measured != null && measured < (segExpected * NarrationTruncationPolicy.MIN_AUDIO_VS_EXPECTED_SPEECH_FRACTION)) {
                anySegmentTruncated = true
            }
        }
        val primaryUrl = segmentUrls.first()
        val aggregateShort =
            totalDurationSec < (expectedSpeechSecs * NarrationTruncationPolicy.MIN_AUDIO_VS_EXPECTED_SPEECH_FRACTION)
        val truncationWarning = anySegmentTruncated || aggregateShort
        return CuratedTtsOutcome(primaryUrl, totalDurationSec.coerceAtLeast(1), segmentUrls, truncationWarning)
    }

    private fun runRewriteStep(translation: StoryTranslation, age: Int, toneMode: ToneMode = ToneMode.CALM): String? {
        if (translation.id > 0 && translation.status != TranslationPipelineStatus.REWRITING) {
            updatePipelineStatus(translation.id, TranslationPipelineStatus.REWRITING, null)
        }
        val timeoutMinutes = appProperties.translationPipeline.rewriteTimeoutMinutes.coerceIn(1, 5)
        log.debug("REWRITE >>> starting masterStoryId={} lang={} timeout={}min", translation.masterStoryId, translation.language, timeoutMinutes)
        val future = CompletableFuture.supplyAsync(
            {
                pipelineMetrics.recordRewriteLatency {
                    rewriteService.rewrite(translation.content, age, toneMode, translation.language)
                }
            },
            pipelineRewriteExecutor
        )
        return try {
            val result = future.get(timeoutMinutes.toLong(), TimeUnit.MINUTES)
            val totalTokens = result.promptTokens + result.completionTokens
            if (totalTokens > 0) narrationMetrics.recordTokenUsage(totalTokens)
            result.scriptText
        } catch (e: TimeoutException) {
            future.cancel(true)
            val msg = "Rewrite timeout after ${appProperties.translationPipeline.rewriteTimeoutMinutes}min"
            log.warn("REWRITE >>> timeout masterStoryId={} lang={}", translation.masterStoryId, translation.language)
            if (translation.id > 0) {
                updatePipelineStatus(translation.id, TranslationPipelineStatus.REWRITE_FAILED, msg)
            }
            throw IllegalStateException(msg)
        } catch (e: Exception) {
            val msg = extractPipelineErrorMessage(e)
            if (translation.id > 0) {
                updatePipelineStatus(translation.id, TranslationPipelineStatus.REWRITE_FAILED, msg)
            }
            throw e
        }
    }

    /**
     * Retry pipeline for missing or failed languages only. Does not reprocess languages that already have audio.
     */
    fun retryFailed(masterStoryId: Long) {
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            val story = storyLibraryRepository.findById(masterStoryId) ?: run {
                log.warn("Master story {} not found for retry", masterStoryId)
                return
            }
            val translations = translationRepository.findByMasterStoryId(masterStoryId)

            // Languages to process: (1) failed/retriable translations, (2) languages with no audio (never started or missing)
            val allowed = pipelineLanguagesForStory(story).toSet()
            val retriableLangs = translations
                .filter {
                    allowed.contains(it.language.trim().lowercase()) &&
                        it.status.canRetry() && it.retryCount < maxRetries
                }
                .map { it.language.trim().lowercase() }
            val missingLangs = pipelineLanguagesForStory(story).filter { !languageHasAudio(masterStoryId, it, story) }
            // Reset retry count for languages that hit max retries but have no audio (manual Retry gives another chance)
            missingLangs.forEach { lang ->
                val trans = translations.find { it.language.equals(lang, ignoreCase = true) }
                if (trans != null && trans.retryCount >= maxRetries) {
                    translationRepository.resetRetryCountByMasterStoryIdAndLanguage(masterStoryId, lang)
                    log.info("Reset retry count for masterStoryId={} lang={} (admin retry)", masterStoryId, lang)
                }
            }
            val languagesToProcess = (retriableLangs + missingLangs).distinct().filter { it in allowed }

            if (languagesToProcess.isEmpty()) {
                log.info("No missing or retriable languages for masterStoryId={}, all pipeline languages have audio", masterStoryId)
                return
            }

            log.info("PIPELINE >>> START masterStoryId={} RETRY languages={}", masterStoryId, languagesToProcess)
            if (!tryClaimOrRecoverStale(masterStoryId)) {
                log.info("PIPELINE >>> SKIP retry masterStoryId={} (already running)", masterStoryId)
                return
            }
            try {
                val translationOnly =
                    appProperties.translationPipeline.audioAfterApproval && story.narrationApprovedAt == null
                processLanguagesInternal(
                    masterStoryId,
                    languagesToProcess,
                    sequential = false,
                    forceRegenerate = false,
                    translationOnly = translationOnly
                )
            } finally {
                progressTracker.clearProcessing(masterStoryId)
            }
        } finally {
            MDC.remove("masterStoryId")
        }
    }

    /**
     * Claim pipeline slot; if in-memory claim is stale but too fresh for age-based clear,
     * recover by validating DB state and clearing claim when no language is actually in progress.
     */
    private fun tryClaimOrRecoverStale(masterStoryId: Long): Boolean {
        if (progressTracker.tryClaimForPipeline(masterStoryId)) return true
        val hasDbInProgress = translationRepository.findByMasterStoryId(masterStoryId)
            .any { it.status in setOf(
                TranslationPipelineStatus.TRANSLATING,
                TranslationPipelineStatus.REWRITING,
                TranslationPipelineStatus.TTS_PROCESSING
            ) }
        if (!hasDbInProgress) {
            progressTracker.clearProcessing(masterStoryId)
            log.warn("PIPELINE >>> cleared stale in-memory claim for masterStoryId={} (no DB in-progress languages)", masterStoryId)
            return progressTracker.tryClaimForPipeline(masterStoryId)
        }
        return false
    }

    /**
     * Republish: run pipeline for specified languages. Does not clear audio upfront.
     * Per language, pipeline skips when audio already READY and content unchanged; only
     * regenerates when content changed or process failed.
     * @param masterStoryId Story to republish
     * @param languages Empty = all supported languages; otherwise only these (e.g. ["te", "hi"])
     */
    fun republishLanguages(masterStoryId: Long, languages: List<String>) {
        log.info("republishLanguages START masterStoryId={} languages={}", masterStoryId, languages)
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            val story = storyLibraryRepository.findById(masterStoryId) ?: run {
                log.warn("Master story {} not found for republish", masterStoryId)
                return
            }
            val allowed = pipelineLanguagesForStory(story).toSet()
            val targetLanguages = if (languages.isEmpty()) {
                log.info("Republish masterStoryId={}: empty languages list → processing all {} pipeline languages", masterStoryId, allowed.size)
                pipelineLanguagesForStory(story)
            } else {
                val normalized = languages.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
                val primary = normalized.filter { it in allowed && (it in supportedLanguages || appProperties.translationPipeline.masterOnlyNarration) }
                when {
                    primary.isNotEmpty() -> primary
                    appProperties.translationPipeline.masterOnlyNarration && normalized.isNotEmpty() -> {
                        val fallback = pipelineLanguagesForStory(story)
                        log.info(
                            "Republish masterStoryId={}: requested langs {} not in master pipeline set; using {}",
                            masterStoryId, normalized, fallback
                        )
                        fallback
                    }
                    else -> normalized.filter { it in supportedLanguages }.filter { it in allowed }
                }
            }
            if (targetLanguages.isEmpty()) {
                log.warn("Republish masterStoryId={}: no valid languages (requested={})", masterStoryId, languages)
                return
            }
            storyLibraryService.invalidateNarrationAudioForLanguages(masterStoryId, targetLanguages)
            log.info("Republish masterStoryId={} languages={} (old audio deleted, running pipeline sequentially)", masterStoryId, targetLanguages)
            try {
                // Republish must bypass idempotent READY-audio skip, because stale DB rows can point to
                // missing S3 objects (e.g. after async invalidation/delete races). Force TTS regeneration.
                processLanguagesInternal(masterStoryId, targetLanguages, sequential = true, forceRegenerate = true)
            } catch (e: Exception) {
                log.error("Republish pipeline failed masterStoryId={} error={}", masterStoryId, e.message, e)
                pipelineMetrics.recordNarrationFailure()
                storyLibraryRepository.findById(masterStoryId)?.let { s ->
                    if (s.status !in listOf("CHANGES_REQUESTED", "REJECTED")) {
                        storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
                    }
                }
            } finally {
                progressTracker.clearProcessing(masterStoryId)
            }
        } finally {
            MDC.remove("masterStoryId")
        }
    }

    /**
     * Regenerate narration: remove old audio, then run TTS-only (no translate/rewrite).
     * Uses existing story_translations content and narration_script; re-synthesizes and uploads audio.
     * Use when audio sounds flat, truncated, or to refresh TTS without re-translating.
     *
     * @param languages When non-empty, only these languages are invalidated and regenerated. When null/empty, all supported languages.
     */
    fun regenerateNarration(masterStoryId: Long, languages: List<String>? = null) {
        val story = storyLibraryRepository.findById(masterStoryId)
            ?: run {
                log.warn("Master story {} not found for regenerate", masterStoryId)
                return
            }
        val translations = translationRepository.findByMasterStoryId(masterStoryId)
        if (translations.isEmpty()) {
            log.warn("No translations for masterStoryId={}, nothing to regenerate", masterStoryId)
            return
        }
        val allowed = pipelineLanguagesForStory(story).toSet()
        val targetLanguages = when {
            languages.isNullOrEmpty() -> {
                log.info("Regenerate narration masterStoryId={}: all {} pipeline languages", masterStoryId, allowed.size)
                pipelineLanguagesForStory(story)
            }
            else -> {
                val normalized = languages.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
                val primary = normalized.filter { it in allowed && (it in supportedLanguages || appProperties.translationPipeline.masterOnlyNarration) }
                when {
                    primary.isNotEmpty() -> primary
                    appProperties.translationPipeline.masterOnlyNarration && normalized.isNotEmpty() -> {
                        val fallback = pipelineLanguagesForStory(story)
                        log.info(
                            "Regenerate narration masterStoryId={}: requested langs {} not in master pipeline set; using {}",
                            masterStoryId, normalized, fallback
                        )
                        fallback
                    }
                    else -> normalized.filter { it in supportedLanguages }.filter { it in allowed }.distinct()
                }
            }
        }
        if (targetLanguages.isEmpty()) {
            log.warn("Regenerate narration masterStoryId={}: no valid languages (requested={})", masterStoryId, languages)
            return
        }
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            storyLibraryService.invalidateNarrationAudioForLanguages(masterStoryId, targetLanguages)
            progressTracker.setProcessing(masterStoryId, "starting")
            log.info("Regenerate narration masterStoryId={} languages={}: old audio deleted, running TTS-only", masterStoryId, targetLanguages)
            processLanguagesInternal(masterStoryId, targetLanguages, forceRegenerate = true)
        } finally {
            MDC.remove("masterStoryId")
        }
    }

    /**
     * Invalidate existing translations and narration audio, then reprocess from updated story content.
     * Use when a curated story is edited so translations/TTS reflect the new content.
     * Uses triggerPipelineExecutor (not @Async) so pipeline reliably starts.
     */
    /**
     * Invalidate content (deletes) in its own transaction so it commits before processSync runs.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun invalidateContent(masterStoryId: Long) {
        storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PROCESSING)
        val translations = translationRepository.findByMasterStoryId(masterStoryId)
        translations.forEach { narrationAudioRepository.deleteByTranslationId(it.id) }
        translationRepository.deleteByMasterStoryId(masterStoryId)
        storyLibraryRepository.clearAudioUrl(masterStoryId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun invalidateAndReprocess(masterStoryId: Long) {
        log.info("Invalidate and reprocess masterStoryId={} (story content edited)", masterStoryId)
        try {
            invalidateContent(masterStoryId)  // commits on return
            val translationOnly = appProperties.translationPipeline.audioAfterApproval
            processSync(masterStoryId, translationOnly = translationOnly)
            log.info("InvalidateAndReprocess pipeline completed for story id={}", masterStoryId)
        } catch (e: Exception) {
            log.error("invalidateAndReprocess FAILED for masterStoryId={} error={} cause={}", masterStoryId, e.message, e.cause?.message, e)
            pipelineMetrics.recordNarrationFailure()
            if (canPipelineUpdateStatus(masterStoryId)) {
                storyLibraryRepository.updateStatus(masterStoryId, StoryStatus.PUBLISHED)
            }
        } finally {
            progressTracker.clearProcessing(masterStoryId)
        }
    }
}
