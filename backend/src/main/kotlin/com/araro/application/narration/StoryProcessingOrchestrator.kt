package com.araro.application.narration

import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.narration.StoryNarrationAudioRepositoryPort
import com.araro.application.port.narration.StoryNarrationScriptRepositoryPort
import com.araro.domain.StoryTranslation
import com.araro.domain.narration.EmotionValidationException
import com.araro.domain.narration.NarrationJobCapacityExceededException
import com.araro.domain.narration.NarrationAudioStatus
import com.araro.domain.narration.StoryNarrationAudio
import com.araro.domain.narration.StoryNarrationScript
import com.araro.domain.narration.ToneMode
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.NarrationMetrics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Orchestrates the full conversational narration pipeline:
 * Format -> Validate -> SSML -> TTS -> Store.
 *
 * Multi-voice: Generates audio per voiceProfile; idempotent per (translation, voice).
 * Prevents duplicate generation via existsByTranslationIdAndVoiceProfileAndStatus(READY).
 * Premium voices validated via NarrationPremiumVoiceValidator when parentId present.
 */
@Service
class StoryProcessingOrchestrator(
    private val appProperties: AppProperties,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val curatedStoryRepository: CuratedStoryRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val narrationFormatter: NarrationFormatterService,
    private val safetyValidator: SafetyValidatorService,
    private val ssmlBuilder: SSMLBuilderService,
    private val emotionTaggingService: EmotionTaggingService,
    private val ttsService: TTSService,
    private val audioStorage: AudioStorageService,
    private val narrationMetrics: NarrationMetrics,
    private val premiumVoiceValidator: NarrationPremiumVoiceValidator,
    private val concurrencyLimiter: NarrationConcurrencyLimiter,
    private val costEstimator: CostEstimatorService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries = 3
    private val sourceLanguage: String
        get() = appProperties.translationPipeline.sourceLanguage.trim().lowercase()

    /**
     * Process narration for a translation. Idempotent per voice profile.
     * @param toneMode CALM (default) or EXPRESSIVE
     * @param voiceProfiles List of voice identifiers; skipped if not entitled
     * @param parentId Optional; required for premium voice validation
     */
    @Transactional
    fun process(
        translation: StoryTranslation,
        toneMode: ToneMode = ToneMode.CALM,
        voiceProfiles: List<String> = listOf("default"),
        parentId: Long? = null
    ) {
        if (!concurrencyLimiter.tryAcquire()) {
            throw NarrationJobCapacityExceededException("Max concurrent narration jobs reached")
        }
        try {
            processInternal(translation, toneMode, voiceProfiles, parentId)
        } finally {
            concurrencyLimiter.release()
        }
    }

    private fun processInternal(
        translation: StoryTranslation,
        toneMode: ToneMode,
        voiceProfiles: List<String>,
        parentId: Long?
    ) {
        val profilesToProcess = voiceProfiles.distinct().filter { premiumVoiceValidator.canUseVoice(it, parentId) }
        if (profilesToProcess.isEmpty()) {
            log.warn("No valid voice profiles for translationId={} (requested: {}, parentId: {})",
                translation.id, voiceProfiles, parentId)
            return
        }
        val skipped = voiceProfiles - profilesToProcess.toSet()
        if (skipped.isNotEmpty()) {
            log.debug("Skipping non-entitled voices for translationId={}: {}", translation.id, skipped)
        }

        val masterStory = curatedStoryRepository.findById(translation.masterStoryId)
            ?: run {
                log.warn("Master story {} not found for narration", translation.masterStoryId)
                return
            }
        val age = masterStory.age

        var retryCount = 0
        var lastError: String? = null

        while (retryCount <= maxRetries) {
            try {
                // Idempotency: skip rewrite if script already exists (no duplicate LLM call)
                val existingScript = narrationScriptRepository.findByTranslationId(translation.id)
                val (scriptText, formatResult) = if (existingScript != null) {
                    log.debug("Narration script exists for translationId={}, idempotent skip", translation.id)
                    existingScript.scriptText to NarrationFormatResult(
                        scriptText = existingScript.scriptText,
                        promptTokens = 0,
                        completionTokens = 0
                    )
                } else {
                    val result = narrationMetrics.recordNarrationLatency {
                        narrationFormatter.formatNarration(
                            translation.content,
                            age,
                            toneMode,
                            translation.language
                        )
                    }
                    result.scriptText to result
                }
                narrationMetrics.recordTokenUsage(formatResult.promptTokens + formatResult.completionTokens)

                try {
                    val validationResult = safetyValidator.validate(
                        SafetyValidationRequest(
                            originalContent = translation.content,
                            originalMoral = translation.moral,
                            formattedScript = scriptText,
                            age = age,
                            language = translation.language
                        )
                    )
                    if (!validationResult.valid) {
                        throw SafetyValidationException("Validation failed", validationResult.violations)
                    }
                } catch (e: SafetyValidationException) {
                    narrationMetrics.recordSafetyValidationFailure()
                    throw e
                }

                // Emotional realism: tag emotions, validate, then build SSML with prosody
                val ssml = try {
                    val emotionTagged = narrationMetrics.recordEmotionTaggingLatency {
                        emotionTaggingService.tagEmotions(scriptText, translation.language)
                    }
                    if (!emotionTagged.validateWordCountTolerance(15)) {
                        throw EmotionValidationException("Emotion tagging word count deviation exceeds 15%")
                    }
                    ssmlBuilder.buildSSMLFromEmotionTagged(emotionTagged, translation.language, age, toneMode)
                } catch (e: EmotionValidationException) {
                    log.debug("Falling back to plain SSML for translationId={}: {}", translation.id, e.message)
                    ssmlBuilder.buildSSML(scriptText, translation.language, age, toneMode)
                }

                // Persist script once (tone mode stored for audit)
                val scriptToUpdate = narrationScriptRepository.findByTranslationId(translation.id)
                val script = StoryNarrationScript(
                    id = scriptToUpdate?.id ?: 0,
                    translationId = translation.id,
                    toneMode = toneMode,
                    scriptText = scriptText,
                    safetyScore = 100,
                    createdAt = scriptToUpdate?.createdAt ?: Instant.now()
                )
                narrationScriptRepository.save(script)

                // Generate and store audio per voice profile
                var anySuccess = false
                for (voiceProfile in profilesToProcess) {
                    if (narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                            translation.id, voiceProfile, NarrationAudioStatus.READY
                        )) {
                        log.debug("Audio already READY for translationId={} voice={}, idempotent skip",
                            translation.id, voiceProfile)
                        anySuccess = true
                        continue
                    }
                    val mp3Bytes = ttsService.synthesize(ssml, translation.language, voiceProfile)
                    if (mp3Bytes == null || mp3Bytes.isEmpty()) {
                        log.warn("TTS returned empty for translationId={} voice={}", translation.id, voiceProfile)
                        saveFailedAudio(translation.id, voiceProfile)
                        continue
                    }
                    val audioUrl = audioStorage.uploadNarrationAudio(
                        translation.masterStoryId,
                        translation.language,
                        voiceProfile,
                        mp3Bytes
                    )
                    val durationSeconds = estimateDurationSeconds(mp3Bytes)
                    val existingAudio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
                    val audio = if (existingAudio != null) {
                        existingAudio.copy(
                            audioUrl = audioUrl,
                            durationSeconds = durationSeconds,
                            status = NarrationAudioStatus.READY,
                            createdAt = Instant.now()  // Fresh timestamp for cache bust; mobile fetches new audio
                        )
                    } else {
                        StoryNarrationAudio(
                            id = 0,
                            translationId = translation.id,
                            voiceProfile = voiceProfile,
                            audioUrl = audioUrl,
                            durationSeconds = durationSeconds,
                            status = NarrationAudioStatus.READY,
                            createdAt = Instant.now()
                        )
                    }
                    narrationAudioRepository.save(audio)
                    anySuccess = true
                    log.info("Narration READY for translationId={} masterStoryId={} lang={} voice={}",
                        translation.id, translation.masterStoryId, translation.language, voiceProfile)
                }
                if (anySuccess) {
                    // Persist conversational script as story text so displayed content matches narrated audio
                    val scriptWordCount = scriptText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    val scriptReadingTime = (scriptWordCount / 150.0).coerceAtMost(5.0)
                    translationRepository.save(
                        translation.copy(
                            content = scriptText,
                            wordCount = scriptWordCount,
                            readingTimeMinutes = scriptReadingTime
                        )
                    )
                    if (translation.language == sourceLanguage) {
                        curatedStoryRepository.updateContent(
                            translation.masterStoryId, scriptText, scriptWordCount, scriptReadingTime
                        )
                    }
                    // Cost estimate: one format + TTS per voice; log once per story
                    costEstimator.estimateAndLog(
                        formatResult.promptTokens,
                        formatResult.completionTokens,
                        ssml.length * profilesToProcess.size,  // Approx: same SSML per voice
                        "translationId=${translation.id}"
                    )
                    return
                }
                throw IllegalStateException("No audio generated for any voice profile")
            } catch (e: Exception) {
                retryCount++
                lastError = e.message ?: "Unknown error"
                log.warn("Narration attempt {} failed for translationId={}: {}", retryCount, translation.id, lastError)
                if (retryCount > maxRetries) {
                    narrationMetrics.recordNarrationFailure()
                    for (voiceProfile in profilesToProcess) {
                        if (!narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                                translation.id, voiceProfile, NarrationAudioStatus.READY)) {
                            saveFailedAudio(translation.id, voiceProfile)
                        }
                    }
                    log.error("Narration FAILED for translationId={} after {} retries: {}",
                        translation.id, maxRetries, lastError)
                    throw RuntimeException("Narration failed: $lastError")
                }
            }
        }
    }

    private fun saveFailedAudio(translationId: Long, voiceProfile: String) {
        val existing = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translationId, voiceProfile)
        val toSave = if (existing != null) {
            existing.copy(status = NarrationAudioStatus.FAILED)
        } else {
            StoryNarrationAudio(
                id = 0,
                translationId = translationId,
                voiceProfile = voiceProfile,
                audioUrl = "",
                durationSeconds = 0,
                status = NarrationAudioStatus.FAILED,
                createdAt = Instant.now()
            )
        }
        narrationAudioRepository.save(toSave)
    }

    private fun estimateDurationSeconds(mp3Bytes: ByteArray): Int {
        return (mp3Bytes.size / 16000).coerceAtLeast(1)
    }
}
