package com.tamixa.infrastructure.pipeline

import com.tamixa.application.narration.GeneratedStoryNarrationService
import com.tamixa.application.port.StoryEventPublisherPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.UsageTrackingPort
import com.tamixa.application.story.StoryIllustrationService
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.StoryStatus
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService

/**
 * In-process story audio processing. Replaces KafkaStoryEventPublisher for Beta.
 * Uses conversational narration pipeline (LLM rewrite + neural TTS) for generated stories.
 */
@Component
@Primary
@Profile("!test")
class InlineStoryEventPublisher(
    private val storyRepository: StoryRepositoryPort,
    private val subscriptionService: SubscriptionService,
    private val usageTrackingPort: UsageTrackingPort,
    private val appProperties: AppProperties,
    private val generatedStoryNarrationService: GeneratedStoryNarrationService,
    private val storyIllustrationService: StoryIllustrationService,
    @Qualifier("narrationTtsExecutor") private val executor: ExecutorService,
    @Value("\${app.audio.simulated-tts-delay-ms:0}") private val simulatedTtsDelayMs: Long
) : StoryEventPublisherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishStoryCreated(storyId: Long, content: String) {
        executor.execute {
            try {
                processStoryAudio(storyId, content)
            } catch (e: Exception) {
                log.error("Audio processing failed for storyId={}", storyId, e)
            }
        }
        executor.execute {
            try {
                val story = storyRepository.findById(storyId) ?: return@execute
                storyIllustrationService.generateCoverForStory(story)
            } catch (e: Exception) {
                log.error("Cover generation failed for storyId={}", storyId, e)
            }
        }
    }

    private fun processStoryAudio(storyId: Long, content: String) {
        val story = storyRepository.findById(storyId) ?: run {
            log.warn("Story storyId={} not found, skipping audio processing", storyId)
            return
        }
        if (story.status == StoryStatus.READY) {
            log.debug("Story storyId={} already READY, idempotent skip", storyId)
            return
        }
        val sub = subscriptionService.getOrCreateSubscription(story.parentId)
        if (!sub.allowsVoicePremium()) {
            val currentMonth = YearMonth.now(ZoneOffset.UTC).toString()
            val usage = usageTrackingPort.getOrCreate(story.parentId, currentMonth)
            val limit = appProperties.subscription.freeVoiceGenerationsPerMonth
            if (usage.voiceGenerations >= limit) {
                log.warn("Voice generation limit reached for parentId={} ({} of {} used)", story.parentId, usage.voiceGenerations, limit)
                storyRepository.updateStatus(storyId, StoryStatus.FAILED)
                return
            }
        }
        storyRepository.updateStatus(storyId, StoryStatus.PROCESSING)
        log.info("Processing conversational narration for storyId={}", storyId)
        try {
            if (simulatedTtsDelayMs > 0) Thread.sleep(simulatedTtsDelayMs)
            val audioFileUrl = generatedStoryNarrationService.processAndStore(storyId)
            if (audioFileUrl != null) {
                storyRepository.updateAudioReady(storyId, audioFileUrl)
                val currentMonth = YearMonth.now(ZoneOffset.UTC).toString()
                usageTrackingPort.incrementVoice(story.parentId, currentMonth)
                log.info("Story storyId={} marked READY with conversational audio path={}", storyId, audioFileUrl)
            } else {
                storyRepository.updateStatus(storyId, StoryStatus.READY)
                log.warn("Narration pipeline failed for storyId={}, marking READY without audio (mobile TTS fallback)", storyId)
            }
        } catch (e: Exception) {
            log.error("Audio processing failed for storyId={}", storyId, e)
            storyRepository.updateStatus(storyId, StoryStatus.READY)
        }
    }
}
