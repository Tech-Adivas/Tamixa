package com.tamixa.application.story

import com.tamixa.application.auth.ParentNotFoundException
import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.StoryCachePort
import com.tamixa.application.port.StoryEventPublisherPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.StoryTokenUsagePort
import com.tamixa.application.port.UsageTrackingPort
import com.tamixa.application.admin.StoryNotFoundException
import com.tamixa.application.guardrail.ExternalGuardrailUnavailableException
import com.tamixa.application.guardrail.GeneratedStoryGuardrailPipeline
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import com.tamixa.domain.SubscriptionPlan
import com.tamixa.domain.SubscriptionStatus
import com.tamixa.controlplane.application.port.ControlPlaneWorkflowService
import com.tamixa.controlplane.application.port.WorkflowExecutionRequest
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.ApplicationMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

private const val WORDS_PER_MINUTE = 120
/** Target: 7–8 minutes of narration per story. */
private const val MAX_READING_MINUTES = 8.0
private const val MAX_WORDS = (WORDS_PER_MINUTE * MAX_READING_MINUTES).toInt()
private const val PLACEHOLDER_CONTENT = "..."

private val ALLOWED_EMOTION_MODES = setOf("CALM", "SOOTHING", "ADVENTUROUS", "DEFAULT")

@Service
class StoryService(
    private val storyRepository: StoryRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val openAI: OpenAIPort,
    private val storyCache: StoryCachePort,
    private val storyEventPublisher: StoryEventPublisherPort,
    private val auditLog: AuditLogPort,
    private val metrics: ApplicationMetrics,
    private val subscriptionService: SubscriptionService,
    private val appProperties: AppProperties,
    private val safetyMiddleware: StorySafetyMiddleware,
    private val storyValidation: StoryValidation,
    private val generatedStoryGuardrailPipeline: GeneratedStoryGuardrailPipeline,
    private val tokenLimitGuard: TokenLimitGuard,
    private val storyPromptBuilder: StoryPromptBuilder,
    private val conversationSummarizer: ConversationSummarizer,
    private val storyTokenUsage: StoryTokenUsagePort,
    private val usageTrackingPort: UsageTrackingPort,
    private val objectMapper: ObjectMapper,
    private val controlPlaneWorkflowService: ControlPlaneWorkflowService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun findByParent(parentEmail: String, page: Int, size: Int): Page<Story> {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        return storyRepository.findByParentId(parent.id, PageRequest.of(page, size.coerceIn(1, 100)))
    }

    /** Search parent's generated stories by theme or title (case-insensitive). */
    fun search(parentEmail: String, query: String, page: Int, size: Int): Page<Story> {
        if (query.isBlank()) return PageImpl(emptyList(), PageRequest.of(0, size.coerceIn(1, 50)), 0)
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        return storyRepository.searchByParentId(parent.id, query, PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 50)))
    }

    fun findByParentAndId(parentEmail: String, storyId: Long): Story {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        val story = storyRepository.findById(storyId)
            ?: throw com.tamixa.application.admin.StoryNotFoundException(storyId)
        if (story.parentId != parent.id) throw StoryAccessDeniedException(storyId)
        return story
    }

    @Transactional
    fun generate(
        parentEmail: String,
        age: Int,
        language: String,
        theme: String,
        childName: String,
        childId: Long?,
        emotionMode: String? = null,
        parentCustomPrompt: String? = null,
        conversationMessages: List<String>? = null,
        learningFocus: String? = null
    ): Story {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw ParentNotFoundException(parentEmail)
        enforceStoryEntitlement(parent.id)
        validateAge(age)

        // Phase 2: Resolve child for personalization (age, language, interests)
        val childContext = resolveChildContext(parent.id, childId, age, language)
        val sanitizedConversation = safetyMiddleware.sanitizeConversationMessages(conversationMessages)
        val conversationSummary = if (sanitizedConversation.isNotEmpty()) {
            conversationSummarizer.summarize(sanitizedConversation)
        } else null
        val baseCustomPrompt = safetyMiddleware.sanitizeParentCustomPrompt(parentCustomPrompt)
        val sanitizedConversationPrompt =
            conversationSummary?.customPrompt?.let { safetyMiddleware.sanitizeParentCustomPrompt(it) }
        val mergedCustomPrompt = mergeCustomPrompts(baseCustomPrompt, sanitizedConversationPrompt)
        val normalizedEmotionMode = conversationSummary?.emotionMode?.takeIf { it in ALLOWED_EMOTION_MODES }
            ?: normalizeEmotionMode(emotionMode)

        // Cost protection: enforce token limits before OpenAI call
        tokenLimitGuard.checkBeforeGeneration(parent.id)
        val sanitized = safetyMiddleware.sanitizeAndValidateInput(theme, childName)
        moderateInput(sanitized.theme, sanitized.childName)

        val promptId = UUID.randomUUID().toString()
        val isPersonalized = childId != null
        val cacheKey = if (isPersonalized) null else buildCacheKey(childContext.age, childContext.language, sanitized.theme, sanitized.childName)

        // Create story row with REQUESTED for atomic status flow
        val requested = Story(
            id = 0,
            parentId = parent.id,
            childId = childId,
            content = PLACEHOLDER_CONTENT,
            theme = sanitized.theme,
            language = childContext.language,
            age = childContext.age,
            childName = sanitized.childName,
            wordCount = 0,
            readingTimeMinutes = 0.0,
            title = null,
            moral = null,
            status = StoryStatus.REQUESTED,
            createdAt = Instant.now(),
            voiceProfileId = null,
            emotionMode = normalizedEmotionMode,
            parentCustomPrompt = mergedCustomPrompt
        )
        val savedRequested = storyRepository.save(requested)
        val storyId = savedRequested.id

        var workflowRunId: UUID? = null
        if (appProperties.controlPlane.storyWorkflowIntegrationEnabled) {
            workflowRunId = controlPlaneWorkflowService.execute(
                WorkflowExecutionRequest(
                    projectCode = appProperties.controlPlane.storyProjectCode,
                    workflowKey = appProperties.controlPlane.storyWorkflowKey,
                    entityType = "STORY",
                    entityId = storyId.toString(),
                    requestedBy = parentEmail,
                    input = mapOf(
                        "theme" to sanitized.theme,
                        "language" to childContext.language,
                        "age" to childContext.age,
                        "childName" to sanitized.childName,
                        "emotionMode" to normalizedEmotionMode,
                        "learningFocus" to learningFocus,
                    ),
                    requestSource = "STORY_SERVICE",
                )
            ).workflowRunId
        }

        try {
        var payloadFromCache = false
        val payload = metrics.recordStoryGenerationLatency {
            val cached = if (cacheKey != null) storyCache.get(cacheKey) else null
            if (cached != null) {
                metrics.recordCacheHit()
                logStoryGenerationEvent(promptId, childContext.language, childContext.age, 0, null, 0)
                payloadFromCache = true
                parseCachedPayload(cached)
            } else {
                metrics.recordCacheMiss()
                storyRepository.updateStatus(storyId, StoryStatus.GENERATING)
                generateWithRetryAndFallback(
                    storyId = storyId,
                    childContext = childContext,
                    theme = sanitized.theme,
                    childName = sanitized.childName,
                    cacheKey = cacheKey,
                    promptId = promptId,
                    emotionMode = normalizedEmotionMode,
                    customPrompt = mergedCustomPrompt,
                    learningFocus = learningFocus
                )
            }
        }

        // Fresh generations are validated before cache write inside generateWithRetryAndFallback; re-validate cache hits
        // so policy changes and corrupt/legacy cache entries cannot bypass StoryValidation.
        if (payloadFromCache) {
            storyValidation.validate(payload, childContext.age)
        }

        val (wordCount, readingTimeMinutes) = computeReadingMeta(payload.storyText)
        enforceReadingTimeLimit(wordCount, readingTimeMinutes)

        storyRepository.updateStatus(storyId, StoryStatus.MODERATION_CHECK)
        val moderationContext = ModerationContext(promptId = promptId, language = childContext.language, age = childContext.age)
        val safetyScore = try {
            generatedStoryGuardrailPipeline.enforceStructuredStoryPayload(payload, moderationContext)
        } catch (e: ContentModerationException) {
            storyRepository.updateStatus(storyId, StoryStatus.FAILED)
            throw e
        } catch (e: ExternalGuardrailUnavailableException) {
            storyRepository.updateStatus(storyId, StoryStatus.FAILED)
            throw e
        }

        val afterModerationStatus =
            if (appProperties.story.humanReviewBeforeNarration) StoryStatus.PENDING_REVIEW else StoryStatus.PENDING

        storyRepository.updateContentAndStatus(
            id = storyId,
            content = payload.storyText,
            title = payload.title,
            moral = payload.moral.ifBlank { null },
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            status = afterModerationStatus,
            safetyScore = safetyScore
        )

        logAiGovernanceStoryCompleted(
            storyId = storyId,
            language = childContext.language,
            safetyScore = safetyScore,
            wordCount = wordCount,
            fromCache = payloadFromCache,
            workflowRunId = workflowRunId,
        )

        if (workflowRunId != null) {
            controlPlaneWorkflowService.markRunCompleted(
                workflowRunId,
                mapOf(
                    "storyId" to storyId,
                    "status" to afterModerationStatus.name,
                    "safetyScore" to safetyScore,
                    "wordCount" to wordCount,
                    "fromCache" to payloadFromCache,
                    "humanReviewRequired" to appProperties.story.humanReviewBeforeNarration,
                ),
            )
        }

        val currentMonth = YearMonth.now(ZoneOffset.UTC).toString()
        usageTrackingPort.incrementStories(parent.id, currentMonth)

        if (appProperties.story.humanReviewBeforeNarration) {
            log.info(
                "story_pending_human_review narration deferred until admin approval storyId={}",
                storyId,
            )
        } else {
            storyEventPublisher.publishStoryCreated(storyId, payload.storyText)
        }
        auditLog.logStoryGeneration(storyId, parent.id, sanitized.theme, traceId = null)
        return storyRepository.findById(storyId) ?: throw StoryNotFoundException(storyId)
        } catch (t: Throwable) {
            if (workflowRunId != null) {
                try {
                    controlPlaneWorkflowService.markRunFailed(
                        workflowRunId,
                        t::class.simpleName,
                        t.message,
                    )
                } catch (e: Exception) {
                    log.warn(
                        "control_plane_mark_run_failed_secondary_failure runId={}",
                        workflowRunId,
                        e,
                    )
                }
            }
            throw t
        }
    }

    /** Child context for personalization (age, language, interests, character builder fields). */
    private data class ChildContext(
        val age: Int,
        val language: String,
        val interests: String?,
        val favoriteColor: String?,
        val favoriteAnimal: String?,
        val characterTraits: String?,
        val avatarChoice: String?
    )

    /** Resolve context for story generation. Child feature removed; uses request params only. */
    private fun resolveChildContext(parentId: Long, childId: Long?, requestAge: Int, requestLang: String): ChildContext =
        ChildContext(requestAge, requestLang, null, null, null, null, null)

    private fun mergeCustomPrompts(base: String?, fromConversation: String?): String? {
        val parts = listOfNotNull(base, fromConversation).filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        return parts.joinToString(" ").trim().take(400)
    }

    private fun normalizeEmotionMode(mode: String?): String? {
        if (mode.isNullOrBlank()) return null
        val upper = mode.trim().uppercase()
        return if (upper in ALLOWED_EMOTION_MODES) upper else null
    }

    /**
     * Generate story with validation; on failure retry once with fallback prompt, then mark FAILED if still failing.
     */
    private fun generateWithRetryAndFallback(
        storyId: Long,
        childContext: ChildContext,
        theme: String,
        childName: String,
        cacheKey: String?,
        promptId: String,
        emotionMode: String? = null,
        customPrompt: String? = null,
        learningFocus: String? = null
    ): StructuredStoryPayload {
        val age = childContext.age
        val language = childContext.language
        val maxWords = storyPromptBuilder.maxWordsForAge(age).coerceAtMost(MAX_WORDS)
        val systemPrompt = storyPromptBuilder.buildSystemMessage()
        val userPrompt = storyPromptBuilder.buildUserPrompt(
            age, language, theme, childName, maxWords, emotionMode, customPrompt,
            childInterests = childContext.interests,
            childFavoriteColor = childContext.favoriteColor,
            childFavoriteAnimal = childContext.favoriteAnimal,
            childTraits = childContext.characterTraits,
            childAvatarChoice = childContext.avatarChoice,
            learningFocus = learningFocus
        )
        val fallbackPrompt = storyPromptBuilder.buildFallbackUserPrompt(age, language, theme, childName, maxWords, emotionMode, customPrompt)

        fun attemptGenerate(userPromptToUse: String, isFallback: Boolean, allowClientFallback: Boolean): StructuredStoryPayload? {
            return try {
                val result = openAI.generateStructuredStory(
                    systemPrompt = systemPrompt,
                    userPrompt = userPromptToUse,
                    fallbackUserPrompt = if (allowClientFallback) fallbackPrompt else null,
                    maxTokens = 1024
                )
                result.usage?.let {
                    metrics.recordTokenUsage(it.totalTokens)
                    storyTokenUsage.recordUsage(storyId, it.promptTokens, it.completionTokens, it.totalTokens)
                    logStoryGenerationEvent(promptId, language, age, it.totalTokens, null, if (isFallback) 1 else 0)
                }
                val generatedWords = result.payload.storyText.split(Regex("\\s+")).count { it.isNotBlank() }
                val estimatedMinutesByWords = generatedWords.toDouble() / WORDS_PER_MINUTE
                val modelEstimatedMinutes = result.payload.estimatedDurationSeconds / 60.0
                log.info(
                    "story_generation_profile promptId={} storyId={} language={} words={} targetMaxWords={} expectedMinutesByWords={} modelEstimatedMinutes={} wpmAssumption={}",
                    promptId,
                    storyId,
                    language,
                    generatedWords,
                    maxWords,
                    String.format("%.2f", estimatedMinutesByWords),
                    String.format("%.2f", modelEstimatedMinutes),
                    WORDS_PER_MINUTE
                )
                storyValidation.validate(result.payload, age)
                cacheKey?.let { storyCache.set(it, objectMapper.writeValueAsString(result.payload)) }
                result.payload
            } catch (e: Exception) {
                if (e is ContentModerationException || e is InvalidStoryRequestException || e is StoryTooLongException) throw e
                null
            }
        }

        var payload = attemptGenerate(userPrompt, isFallback = false, allowClientFallback = true)
        if (payload == null) {
            metrics.recordValidationFailure()
            metrics.recordFallbackUsage()
            payload = attemptGenerate(fallbackPrompt, isFallback = true, allowClientFallback = false)
        }
        if (payload == null) {
            storyRepository.updateStatus(storyId, StoryStatus.FAILED)
            metrics.recordValidationFailure()
            log.warn("Story generation failed after fallback prompt, storyId={} promptId={}", storyId, promptId)
            throw InvalidStoryRequestException("Story generation failed after retry. Please try again.")
        }
        return payload
    }

    private fun logStoryGenerationEvent(
        promptId: String,
        language: String,
        age: Int,
        tokenUsage: Int,
        moderationSafe: Boolean?,
        retryCount: Int
    ) {
        log.info(
            "story_generation",
            StructuredArguments.kv("promptId", promptId),
            StructuredArguments.kv("language", language),
            StructuredArguments.kv("age", age),
            StructuredArguments.kv("tokenUsage", tokenUsage),
            StructuredArguments.kv("moderationSafe", moderationSafe),
            StructuredArguments.kv("retryCount", retryCount)
        )
    }

    /**
     * Structured line for observability / evaluation pipelines (AI SDLC docs: docs/AI_EVALUATION_SYSTEM.md).
     * Query logs with key `event=ai_governance_output`.
     */
    private fun logAiGovernanceStoryCompleted(
        storyId: Long,
        language: String,
        safetyScore: Int?,
        wordCount: Int,
        fromCache: Boolean,
        workflowRunId: UUID?,
    ) {
        log.info(
            "ai_governance_output",
            StructuredArguments.kv("event", "ai_governance_output"),
            StructuredArguments.kv("outputType", "generated_story"),
            StructuredArguments.kv("taskClass", "story_generation"),
            StructuredArguments.kv("storyId", storyId),
            StructuredArguments.kv("language", language),
            StructuredArguments.kv("safetyScore", safetyScore),
            StructuredArguments.kv("wordCount", wordCount),
            StructuredArguments.kv("fromCache", fromCache),
            StructuredArguments.kv("workflowRunId", workflowRunId),
        )
    }

    private fun parseCachedPayload(cached: String): StructuredStoryPayload {
        return try {
            objectMapper.readValue(cached, StructuredStoryPayload::class.java)
        } catch (e: Exception) {
            metrics.recordValidationFailure()
            log.warn("Cache payload parse failed, treating as plain story text: {}", e.message)
            StructuredStoryPayload(
                title = "Story",
                moral = "",
                storyText = cached,
                estimatedDurationSeconds = (cached.split(Regex("\\s+")).filter { it.isNotBlank() }.size.toDouble() / WORDS_PER_MINUTE * 60).toInt().coerceAtLeast(1)
            )
        }
    }

    private fun buildCacheKey(age: Int, language: String, theme: String, childName: String): String {
        val input = "$age:$language:${theme.lowercase()}:${childName.lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Server-side enforcement: validate subscription state, then enforce FREE/STARTER plan limits.
     * Financial safety: reject EXPIRED, CANCELED, PAYMENT_FAILED before any story generation.
     */
    private fun enforceStoryEntitlement(parentId: Long) {
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        // Subscription state validation: block access for terminal/invalid states
        if (sub.status in listOf(SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELED, SubscriptionStatus.PAYMENT_FAILED)) {
            throw SubscriptionStateBlockedException(sub.status)
        }
        val limit = when (sub.plan) {
            SubscriptionPlan.FREE -> appProperties.subscription.freeStoriesPerMonth
            SubscriptionPlan.STARTER -> appProperties.subscription.starterStoriesPerMonth
            else -> return  // PREMIUM_MONTHLY, PREMIUM_YEARLY, FAMILY, VOICE_PREMIUM: unlimited
        }
        val currentMonth = YearMonth.now(ZoneOffset.UTC).toString()
        val usage = usageTrackingPort.getOrCreate(parentId, currentMonth)
        if (usage.storiesGenerated >= limit) {
            throw FreeStoryLimitReachedException(
                limit = limit,
                remaining = 0,
                recommendedPlan = if (sub.plan == SubscriptionPlan.FREE) "PREMIUM" else "PREMIUM"
            )
        }
    }

    private fun validateAge(age: Int) {
        if (age < 1 || age > 12) throw InvalidStoryRequestException("Age must be between 1 and 12")
    }

    private fun moderateInput(theme: String, childName: String) {
        val textToCheck = "$theme $childName"
        val result = openAI.getModerationResult(textToCheck)
        if (!result.safe) {
            throw ContentModerationException("Theme or child name failed content moderation")
        }
        if (result.categories.violence || result.categories.violenceGraphic) {
            throw ContentModerationException("Content not allowed for children")
        }
    }

    private fun computeReadingMeta(content: String): Pair<Int, Double> {
        val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val readingTimeMinutes = wordCount.toDouble() / WORDS_PER_MINUTE
        return wordCount to readingTimeMinutes
    }

    private fun enforceReadingTimeLimit(wordCount: Int, readingTimeMinutes: Double) {
        if (readingTimeMinutes > MAX_READING_MINUTES) {
            throw StoryTooLongException(
                "Story exceeds $MAX_READING_MINUTES minutes reading time (${"%.1f".format(readingTimeMinutes)} min, $wordCount words). Max $MAX_WORDS words."
            )
        }
    }
}

class StoryAccessDeniedException(storyId: Long) : RuntimeException("Access denied to story: $storyId")

class ContentModerationException(message: String) : RuntimeException(message)
class InvalidStoryRequestException(message: String) : RuntimeException(message)
class StoryTooLongException(message: String) : RuntimeException(message)

/** Thrown when subscription state blocks access (EXPIRED, CANCELED, PAYMENT_FAILED). */
class SubscriptionStateBlockedException(status: SubscriptionStatus) :
    RuntimeException("Subscription state $status does not allow story generation")
/**
 * Thrown when free usage limit reached. Carries structured data for upgrade nudge.
 * Handler returns LimitReachedResponse so mobile UI can show upgrade CTA.
 */
class FreeStoryLimitReachedException(
    val limit: Int,
    val remaining: Int = 0,
    val recommendedPlan: String = "PREMIUM"
) : RuntimeException("Free plan limit reached: $limit stories per month. Upgrade to Premium for unlimited stories.")
