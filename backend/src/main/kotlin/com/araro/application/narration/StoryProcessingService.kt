package com.araro.application.narration

import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryAudioRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.narration.StoryNarrationAudioRepositoryPort
import com.araro.application.translation.TranslationService
import com.araro.application.narration.EmotionToToneMapper
import com.araro.domain.StoryTranslation
import com.araro.domain.narration.EmotionValidationException
import com.araro.domain.TranslationPipelineStatus
import com.araro.domain.narration.NarrationAudioStatus
import com.araro.domain.narration.ToneMode
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.NarrationPipelineMetrics
import com.araro.infrastructure.observability.PipelineProgressTracker
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
import java.util.concurrent.ExecutorService

/**
 * Production-grade narration pipeline: pre-generate all story audio at publish time.
 * 2-step LLM: Translate → Rewrite conversationally. Neural TTS only. No voice cloning, no HLS.
 * Idempotent per (storyId, language, voice). Retry max 3 times with exponential backoff (2s, 5s, 10s).
 */
@Service
class StoryProcessingService(
    private val curatedStoryRepository: CuratedStoryRepositoryPort,
    private val storyAudioRepository: StoryAudioRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val translationService: TranslationService,
    private val rewriteService: RewriteService,
    private val emotionTaggingService: EmotionTaggingService,
    private val ssmlBuilder: SSMLBuilderService,
    private val ttsService: TTSService,
    private val audioStorage: AudioStorageService,
    private val appProperties: AppProperties,
    private val pipelineMetrics: NarrationPipelineMetrics,
    private val ttsConcurrencyLimiter: TtsConcurrencyLimiter,
    private val progressTracker: PipelineProgressTracker,
    @Qualifier("pipelineLanguageExecutor") private val pipelineLanguageExecutor: ExecutorService,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor,
    @Lazy private val self: StoryProcessingService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries = 3
    private val supportedLanguages: List<String>
        get() = (listOf(sourceLanguage) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()
    private val sourceLanguage: String
        get() = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
    private val defaultVoice = "default"

    /** Run status update in a separate transaction to avoid persistence-context conflicts with @Modifying queries. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun atomicStatusUpdateInNewTx(translationId: Long, status: TranslationPipelineStatus, lastError: String?) {
        translationRepository.atomicStatusUpdate(translationId, status, lastError)
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

    /** Returns true if language has playable audio (curated_stories.audio_file_url for source, else narration READY). */
    private fun languageHasAudio(masterStoryId: Long, lang: String, story: com.araro.domain.CuratedStory): Boolean {
        if (lang == sourceLanguage && !story.audioFileUrl.isNullOrBlank()) return true
        val translation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, lang) ?: return false
        return narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
            translation.id, defaultVoice, NarrationAudioStatus.READY
        )
    }

    /** Process only the given languages. Used for retry and republish. Runs pipeline in parallel. */
    private fun processLanguagesInternal(masterStoryId: Long, languagesToProcess: List<String>) {
        if (languagesToProcess.isEmpty()) {
            log.warn("processLanguagesInternal: empty languages list, skipping masterStoryId={}", masterStoryId)
            return
        }
        val story = curatedStoryRepository.findById(masterStoryId)
            ?: run {
                log.warn("Master story {} not found for processing", masterStoryId)
                return
            }
        curatedStoryRepository.updateStatus(masterStoryId, "PROCESSING")
        log.info("Pipeline START masterStoryId={} languages={} (submitting {} tasks)", masterStoryId, languagesToProcess, languagesToProcess.size)
        val toneMode = EmotionToToneMapper.toToneMode(story.emotionMode)
        val tasks = languagesToProcess.map { language ->
            Callable {
                MDC.put("masterStoryId", masterStoryId.toString())
                MDC.put("language", language)
                try {
                    val langStart = System.nanoTime()
                    self.processLanguage(masterStoryId, language, story.content, story.title, story.moral, story.age, toneMode)
                    val langMs = (System.nanoTime() - langStart) / 1_000_000
                    log.info("Pipeline masterStoryId={} lang={} done in {}ms", masterStoryId, language, langMs)
                } finally {
                    MDC.remove("language")
                }
            }
        }
        pipelineLanguageExecutor.invokeAll(tasks).forEach { it.get() }

        val storyNow = curatedStoryRepository.findById(masterStoryId)!!
        val allLangsHaveAudio = supportedLanguages.all { languageHasAudio(masterStoryId, it, storyNow) }
        if (allLangsHaveAudio) {
            curatedStoryRepository.updateStatus(masterStoryId, "READY")
            pipelineMetrics.recordDailyGeneration()
            log.info("Story masterStoryId={} READY: all {} languages have audio", masterStoryId, supportedLanguages.size)
        } else {
            val translations = translationRepository.findByMasterStoryId(masterStoryId)
            val statusesStr = translations.joinToString(",") { "${it.language}=${it.status}" }
            log.warn("Story masterStoryId={} not ready: statusByLang=[{}]", masterStoryId, statusesStr)
            curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
        }
    }

    /**
     * Run pipeline synchronously in caller thread. Use when async does not run (e.g. story stuck at PENDING).
     * Blocks until complete; use for debugging or manual recovery.
     * Uses REQUIRES_NEW so pipeline has its own transaction (avoids long transaction from caller).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processSync(masterStoryId: Long) {
        log.info("processSync START masterStoryId={} (blocking)", masterStoryId)
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            processLanguagesInternal(masterStoryId, supportedLanguages)
        } catch (e: Exception) {
            log.error("processSync failed masterStoryId={} error={}", masterStoryId, e.message, e)
            pipelineMetrics.recordNarrationFailure()
            curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
        } finally {
            progressTracker.clearProcessing(masterStoryId)
            MDC.remove("masterStoryId")
        }
    }

    /** Process only the given languages. Used for retry. */
    @Async
    @Transactional
    fun processLanguagesAsync(masterStoryId: Long, languagesToProcess: List<String>) {
        MDC.put("masterStoryId", masterStoryId.toString())
        val start = System.nanoTime()
        try {
            processLanguagesInternal(masterStoryId, languagesToProcess)
        } catch (e: Exception) {
            log.error("Pipeline failed for masterStoryId={} error={} cause={}", masterStoryId, e.message, e.cause?.message, e)
            pipelineMetrics.recordNarrationFailure()
            curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
        } finally {
            progressTracker.clearProcessing(masterStoryId)
            pipelineMetrics.recordNarrationLatency(System.nanoTime() - start)
            MDC.remove("masterStoryId")
        }
    }

    /**
     * Trigger async processing when admin publishes story.
     * Sets status to PROCESSING, processes all languages, marks READY when all COMPLETED.
     */
    @Async
    @Transactional
    fun processAsync(masterStoryId: Long) {
        log.info("processAsync ASYNC START masterStoryId={}", masterStoryId)
        MDC.put("masterStoryId", masterStoryId.toString())
        val start = System.nanoTime()
        try {
            val story = curatedStoryRepository.findById(masterStoryId)
                ?: run {
                    log.warn("Master story {} not found for processing", masterStoryId)
                    return
                }
            curatedStoryRepository.updateStatus(masterStoryId, "PROCESSING")
            val languages = supportedLanguages
            log.info("Pipeline START masterStoryId={} languages={} (parallel)", masterStoryId, languages)
            val toneMode = EmotionToToneMapper.toToneMode(story.emotionMode)
            val tasks = languages.map { language ->
                Callable {
                    MDC.put("masterStoryId", masterStoryId.toString())
                    MDC.put("language", language)
                    try {
                        val langStart = System.nanoTime()
                        self.processLanguage(masterStoryId, language, story.content, story.title, story.moral, story.age, toneMode)
                        val langMs = (System.nanoTime() - langStart) / 1_000_000
                        log.info("Pipeline masterStoryId={} lang={} done in {}ms", masterStoryId, language, langMs)
                    } finally {
                        MDC.remove("language")
                    }
                }
            }
            pipelineLanguageExecutor.invokeAll(tasks).forEach { it.get() }

            val storyNow = curatedStoryRepository.findById(masterStoryId)!!
            val allLangsHaveAudio = supportedLanguages.all { languageHasAudio(masterStoryId, it, storyNow) }
            if (allLangsHaveAudio) {
                curatedStoryRepository.updateStatus(masterStoryId, "READY")
                pipelineMetrics.recordDailyGeneration()
                val totalMs = (System.nanoTime() - start) / 1_000_000
                log.info("Story masterStoryId={} READY: all {} languages have audio in {}ms (~{} min)", masterStoryId, supportedLanguages.size, totalMs, totalMs / 60000)
            } else {
                val missingLangs = supportedLanguages.filter { !languageHasAudio(masterStoryId, it, storyNow) }
                val allTranslations = translationRepository.findByMasterStoryId(masterStoryId)
                val statusesStr = allTranslations.joinToString(",") { "${it.language}=${it.status}" }
                log.warn("Story masterStoryId={} not ready: missing languages {} statusByLang=[{}]", masterStoryId, missingLangs, statusesStr)
                curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
            }
        } catch (e: Exception) {
            log.error("Story processing failed for masterStoryId={} error={} cause={}", masterStoryId, e.message, e.cause?.message, e)
            pipelineMetrics.recordNarrationFailure()
            curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
        } finally {
            progressTracker.clearProcessing(masterStoryId)
            pipelineMetrics.recordNarrationLatency(System.nanoTime() - start)
            MDC.remove("masterStoryId")
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processLanguage(
        masterStoryId: Long,
        language: String,
        sourceContent: String,
        sourceTitle: String?,
        sourceMoral: String?,
        age: Int,
        toneMode: ToneMode = ToneMode.CALM
    ) {
        log.info("processLanguage START masterStoryId={} lang={}", masterStoryId, language)
        val translation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)

        if (translation != null && translation.status == TranslationPipelineStatus.COMPLETED) {
            if (narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                    translation.id, defaultVoice, NarrationAudioStatus.READY)) {
                log.debug("Idempotent skip: audio exists for masterStoryId={} lang={}", masterStoryId, language)
                return
            }
        }

        var currentTranslation = translation
        var retryCount = translation?.retryCount ?: 0
        if (retryCount >= maxRetries) {
            log.warn("Max retries reached for masterStoryId={} lang={}", masterStoryId, language)
            return
        }

        for (attempt in 1..(maxRetries - retryCount)) {
            try {
                val contentToProcess = runTranslationStep(
                    masterStoryId, language, sourceContent, sourceTitle, sourceMoral, age, currentTranslation
                ) ?: continue
                currentTranslation = translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)!!

                progressTracker.setProcessing(masterStoryId, language)
                val scriptText = runRewriteStep(currentTranslation, age, toneMode) ?: continue

                if (narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                        currentTranslation.id, defaultVoice, NarrationAudioStatus.READY)) {
                    self.atomicStatusUpdateInNewTx(
                        currentTranslation.id, TranslationPipelineStatus.COMPLETED, null
                    )
                    return
                }

                val ssml = buildSsmlWithEmotionTagging(scriptText, language, age, toneMode, currentTranslation.id)

                self.atomicStatusUpdateInNewTx(currentTranslation.id, TranslationPipelineStatus.TTS_PROCESSING, null)
                log.info("TTS starting masterStoryId={} lang={} (acquiring permit, then calling TTS API)", masterStoryId, language)
                val ttsStartMs = System.currentTimeMillis()
                val audioBytes = ttsConcurrencyLimiter.withPermit(
                    block = { ttsService.synthesize(ssml, language, defaultVoice) }
                ) ?: throw IllegalStateException("TTS returned null")
                if (audioBytes.isEmpty()) throw IllegalStateException("TTS returned empty")
                val ttsMs = System.currentTimeMillis() - ttsStartMs
                log.info("TTS_PROCESSING masterStoryId={} lang={} synthesized {} bytes in {}ms", masterStoryId, language, audioBytes.size, ttsMs)

                val audioUrl = audioStorage.uploadNarrationAudio(
                    masterStoryId, language, defaultVoice, audioBytes
                )
                val durationSeconds = (audioBytes.size / 16000).coerceAtLeast(1)

                val existingAudio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(
                    currentTranslation.id, defaultVoice
                )
                val audio = if (existingAudio != null) {
                    com.araro.domain.narration.StoryNarrationAudio(
                        id = existingAudio.id,
                        translationId = currentTranslation.id,
                        voiceProfile = defaultVoice,
                        audioUrl = audioUrl,
                        durationSeconds = durationSeconds,
                        status = NarrationAudioStatus.READY,
                        createdAt = Instant.now()  // Fresh timestamp so ?v= cache bust changes; forces mobile to fetch new audio
                    )
                } else {
                    com.araro.domain.narration.StoryNarrationAudio(
                        id = 0,
                        translationId = currentTranslation.id,
                        voiceProfile = defaultVoice,
                        audioUrl = audioUrl,
                        durationSeconds = durationSeconds,
                        status = NarrationAudioStatus.READY,
                        createdAt = Instant.now()
                    )
                }
                narrationAudioRepository.save(audio)

                if (language == sourceLanguage) {
                    curatedStoryRepository.updateAudioUrl(masterStoryId, audioUrl)
                }

                // Persist conversational script as story text so displayed content matches narrated audio
                val scriptWordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val scriptReadingTime = (scriptWordCount / 150.0).coerceAtMost(5.0)
                translationRepository.save(
                    currentTranslation.copy(
                        content = scriptText,
                        wordCount = scriptWordCount,
                        readingTimeMinutes = scriptReadingTime
                    )
                )
                if (language == sourceLanguage) {
                    curatedStoryRepository.updateContent(masterStoryId, scriptText, scriptWordCount, scriptReadingTime)
                }

                self.atomicStatusUpdateInNewTx(
                    currentTranslation.id,
                    TranslationPipelineStatus.COMPLETED,
                    null
                )
                pipelineMetrics.recordTtsLatency(ttsMs * 1_000_000L)
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
                // Persist failure in a new transaction; the current tx is rollback-only after the exception.
                self.persistProcessingFailure(
                    currentTranslation = currentTranslation,
                    masterStoryId = masterStoryId,
                    language = language,
                    failedStatus = failedStatus,
                    errorMsg = errorMsg,
                    retryCount = retryCount
                )
                pipelineMetrics.recordNarrationFailure()
                if (retryCount >= maxRetries) {
                    log.error("Pipeline FAILED for masterStoryId={} lang={} after {} retries",
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
            translationRepository.incrementRetryCount(currentTranslation.id, errorMsg)
            translationRepository.atomicStatusUpdate(currentTranslation.id, failedStatus, errorMsg)
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

    private val failedPlaceholderContent = "[Pipeline failed]"

    /**
     * Build SSML with emotion-tagged prosody when possible (less flat). Falls back to plain SSML
     * if emotion tagging validation fails—same as StoryProcessingOrchestrator.
     */
    private fun buildSsmlWithEmotionTagging(
        scriptText: String,
        language: String,
        age: Int,
        toneMode: ToneMode,
        translationId: Long
    ): String = try {
        val emotionTagged = emotionTaggingService.tagEmotions(scriptText, language)
        if (!emotionTagged.validateWordCountTolerance(15)) {
            throw EmotionValidationException("Emotion tagging word count deviation exceeds 15%")
        }
        ssmlBuilder.buildSSMLFromEmotionTagged(emotionTagged, language, age, toneMode)
    } catch (e: EmotionValidationException) {
        log.debug("Falling back to plain SSML for translationId={}: {}", translationId, e.message)
        ssmlBuilder.buildSSML(scriptText, language, age, toneMode)
    }

    private fun runTranslationStep(
        masterStoryId: Long,
        language: String,
        sourceContent: String,
        sourceTitle: String?,
        sourceMoral: String?,
        age: Int,
        existing: StoryTranslation?
    ): String? {
        return try {
            if (existing != null && existing.content.isNotBlank() && !existing.content.startsWith(failedPlaceholderContent)) {
                self.atomicStatusUpdateInNewTx(existing.id, TranslationPipelineStatus.REWRITING, null)
                return existing.content
            }
            if (existing != null) {
                self.atomicStatusUpdateInNewTx(existing.id, TranslationPipelineStatus.TRANSLATING, null)
            }
            val result = pipelineMetrics.recordTranslationLatency {
                translationService.translateIfNeeded(
                    sourceLang = sourceLanguage,
                    targetLang = language,
                    title = sourceTitle,
                    content = sourceContent,
                    moral = sourceMoral
                )
            }
            val wordCount = result.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
            val toSave = if (existing != null) {
                existing.copy(
                    content = result.content,
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
                    content = result.content,
                    moral = result.moral,
                    wordCount = wordCount,
                    readingTimeMinutes = readingTimeMinutes,
                    createdAt = Instant.now(),
                    status = TranslationPipelineStatus.REWRITING
                )
            }
            translationRepository.save(toSave)
            result.content
        } catch (e: Exception) {
            val msg = extractPipelineErrorMessage(e)
            translationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.let {
                self.atomicStatusUpdateInNewTx(it.id, TranslationPipelineStatus.TRANSLATION_FAILED, msg)
            }
            throw e
        }
    }

    private fun runRewriteStep(translation: StoryTranslation, age: Int, toneMode: ToneMode = ToneMode.CALM): String? {
        return try {
            if (translation.id > 0) {
                self.atomicStatusUpdateInNewTx(translation.id, TranslationPipelineStatus.REWRITING, null)
            }
            val result = pipelineMetrics.recordRewriteLatency {
                rewriteService.rewrite(translation.content, age, toneMode, translation.language)
            }
            if (translation.id > 0) {
                self.atomicStatusUpdateInNewTx(translation.id, TranslationPipelineStatus.TTS_PROCESSING, null)
            }
            result.scriptText
        } catch (e: Exception) {
            val msg = extractPipelineErrorMessage(e)
            if (translation.id > 0) {
                self.atomicStatusUpdateInNewTx(translation.id, TranslationPipelineStatus.REWRITE_FAILED, msg)
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
            val story = curatedStoryRepository.findById(masterStoryId) ?: run {
                log.warn("Master story {} not found for retry", masterStoryId)
                return
            }
            val translations = translationRepository.findByMasterStoryId(masterStoryId)

            // Languages to process: (1) failed/retriable translations, (2) languages with no audio (never started or missing)
            val retriableLangs = translations
                .filter { it.status.canRetry() && it.retryCount < maxRetries }
                .map { it.language }
            val missingLangs = supportedLanguages.filter { !languageHasAudio(masterStoryId, it, story) }
            // Reset retry count for languages that hit max retries but have no audio (manual Retry gives another chance)
            missingLangs.forEach { lang ->
                val trans = translations.find { it.language == lang }
                if (trans != null && trans.retryCount >= maxRetries) {
                    translationRepository.resetRetryCountByMasterStoryIdAndLanguage(masterStoryId, lang)
                    log.info("Reset retry count for masterStoryId={} lang={} (admin retry)", masterStoryId, lang)
                }
            }
            val languagesToProcess = (retriableLangs + missingLangs).distinct()

            if (languagesToProcess.isEmpty()) {
                log.info("No missing or retriable languages for masterStoryId={}, all {} languages have audio", masterStoryId, supportedLanguages.size)
                return
            }

            log.info("Retry masterStoryId={} languages={} (missing/failed only, skipping {} completed)",
                masterStoryId, languagesToProcess, supportedLanguages.size - languagesToProcess.size)
            processLanguagesInternal(masterStoryId, languagesToProcess)
        } finally {
            MDC.remove("masterStoryId")
        }
    }

    /** Clear audio for republish in own transaction so workers see committed deletes. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun republishClearAudio(masterStoryId: Long, targetLanguages: List<String>) {
        curatedStoryRepository.updateStatus(masterStoryId, "PROCESSING")
        val translations = translationRepository.findByMasterStoryId(masterStoryId)
        for (lang in targetLanguages) {
            if (lang == sourceLanguage) curatedStoryRepository.clearAudioUrl(masterStoryId)
            storyAudioRepository.deleteByMasterStoryIdAndLanguage(masterStoryId, lang)
            val trans = translations.find { it.language == lang }
            if (trans != null) {
                narrationAudioRepository.deleteByTranslationId(trans.id)
                translationRepository.resetRetryCountByMasterStoryIdAndLanguage(masterStoryId, lang)
                translationRepository.atomicStatusUpdate(trans.id, TranslationPipelineStatus.PENDING, null)
            }
        }
    }

    /**
     * Republish: clear audio for specified languages and reprocess. Use when you want a fresh
     * run for specific languages (e.g. Telugu only) or all.
     * Caller should schedule in executor to avoid blocking. No @Async.
     * @param masterStoryId Story to republish
     * @param languages Empty = all supported languages; otherwise only these (e.g. ["te", "hi"])
     */
    @Transactional
    fun republishLanguages(masterStoryId: Long, languages: List<String>) {
        log.info("republishLanguages START masterStoryId={} languages={}", masterStoryId, languages)
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            val story = curatedStoryRepository.findById(masterStoryId) ?: run {
                log.warn("Master story {} not found for republish", masterStoryId)
                return
            }
            val targetLanguages = if (languages.isEmpty()) {
                log.info("Republish masterStoryId={}: empty languages list → processing all {} supported languages", masterStoryId, supportedLanguages.size)
                supportedLanguages
            } else {
                languages.map { it.trim().lowercase() }.filter { it in supportedLanguages }
            }
            if (targetLanguages.isEmpty()) {
                log.warn("Republish masterStoryId={}: no valid languages (requested={})", masterStoryId, languages)
                return
            }
            self.republishClearAudio(masterStoryId, targetLanguages)  // commits before workers run
            log.info("Republish masterStoryId={} languages={} (cleared audio, reprocessing)",
                masterStoryId, targetLanguages)
            try {
                processLanguagesInternal(masterStoryId, targetLanguages)
            } catch (e: Exception) {
                log.error("Republish pipeline failed masterStoryId={} error={}", masterStoryId, e.message, e)
                pipelineMetrics.recordNarrationFailure()
                curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
            } finally {
                progressTracker.clearProcessing(masterStoryId)
            }
        } finally {
            MDC.remove("masterStoryId")
        }
    }

    /**
     * Regenerate narration audio for all languages. Clears existing audio and reprocesses
     * (rewrite + TTS) using current translations. Use when audio sounds flat—e.g. after fixing
     * rewrite/emotion-tagging pipeline. Keeps translations; only redoes rewrite and TTS.
     * Caller should schedule in executor to avoid blocking. No @Async.
     */
    @Transactional
    fun regenerateNarration(masterStoryId: Long) {
        val story = curatedStoryRepository.findById(masterStoryId)
            ?: run {
                log.warn("Master story {} not found for regenerate", masterStoryId)
                return
            }
        val translations = translationRepository.findByMasterStoryId(masterStoryId)
        if (translations.isEmpty()) {
            log.warn("No translations for masterStoryId={}, nothing to regenerate", masterStoryId)
            return
        }
        MDC.put("masterStoryId", masterStoryId.toString())
        try {
            translations.forEach { narrationAudioRepository.deleteByTranslationId(it.id) }
            curatedStoryRepository.clearAudioUrl(masterStoryId)
            translationRepository.resetRetryCountByMasterStoryId(masterStoryId)
            log.info("Regenerate narration masterStoryId={}: cleared audio for {} translations, reprocessing",
                masterStoryId, translations.size)
            processLanguagesInternal(masterStoryId, supportedLanguages)
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
        curatedStoryRepository.updateStatus(masterStoryId, "PROCESSING")
        val translations = translationRepository.findByMasterStoryId(masterStoryId)
        translations.forEach { narrationAudioRepository.deleteByTranslationId(it.id) }
        translationRepository.deleteByMasterStoryId(masterStoryId)
        curatedStoryRepository.clearAudioUrl(masterStoryId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun invalidateAndReprocess(masterStoryId: Long) {
        log.info("Invalidate and reprocess masterStoryId={} (story content edited)", masterStoryId)
        try {
            invalidateContent(masterStoryId)  // commits on return
            processSync(masterStoryId)        // runs in new tx, sees committed deletes
            log.info("InvalidateAndReprocess pipeline completed for story id={}", masterStoryId)
        } catch (e: Exception) {
            log.error("invalidateAndReprocess FAILED for masterStoryId={} error={} cause={}", masterStoryId, e.message, e.cause?.message, e)
            pipelineMetrics.recordNarrationFailure()
            curatedStoryRepository.updateStatus(masterStoryId, "PUBLISHED")
        } finally {
            progressTracker.clearProcessing(masterStoryId)
        }
    }
}
