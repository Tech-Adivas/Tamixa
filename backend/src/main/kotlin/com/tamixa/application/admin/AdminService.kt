package com.tamixa.application.admin

import com.tamixa.api.admin.dto.AddAdminUserRequest
import com.tamixa.api.admin.dto.AdminInvoiceDto
import com.tamixa.api.admin.dto.AdminUserDto
import com.tamixa.api.admin.dto.AiMetricsDto
import com.tamixa.api.admin.dto.ApiUsageDto
import com.tamixa.api.admin.dto.StoryAiUsageDto
import com.tamixa.application.analytics.CompletionMetricsDto
import com.tamixa.application.analytics.RetentionMetricsDto
import com.tamixa.api.admin.dto.ChildSummaryDto
import com.tamixa.api.admin.dto.HealthDto
import com.tamixa.api.admin.dto.PagedResponse
import com.tamixa.api.admin.dto.CreateParentRequest
import com.tamixa.api.admin.dto.ParentDetailDto
import com.tamixa.api.admin.dto.ParentSummaryDto
import com.tamixa.api.admin.dto.UpdateParentRequest
import com.tamixa.api.admin.dto.RevenueRowDto
import com.tamixa.api.admin.dto.StoryDetailDto
import com.tamixa.api.admin.dto.StorySummaryDto
import com.tamixa.api.admin.dto.StoryUsagePointDto
import com.tamixa.api.admin.dto.SubscriptionMetricsDto
import com.tamixa.api.admin.dto.SubscriptionStatusDto
import com.tamixa.api.admin.dto.VoiceUploadLogDto
import com.tamixa.api.config.RequestTracingFilter
import com.tamixa.application.auth.EmailAlreadyExistsException
import com.tamixa.application.auth.PhoneAlreadyInUseException
import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.StoryEventPublisherPort
import com.tamixa.application.subscription.RevenueMetricsService
import com.tamixa.application.port.RevenueSnapshotPort
import com.tamixa.domain.InvoiceStatus
import com.tamixa.domain.Role
import com.tamixa.domain.StoryStatus
import com.tamixa.domain.SubscriptionPlan
import com.tamixa.domain.SubscriptionStatus
import com.tamixa.infrastructure.logging.PiiMask
import com.tamixa.infrastructure.persistence.AdminAuditEntity
import org.slf4j.LoggerFactory
import com.tamixa.infrastructure.persistence.AdminAuditJpaRepository
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.InvoiceEntity
import com.tamixa.infrastructure.persistence.InvoiceJpaRepository
import com.tamixa.infrastructure.persistence.ParentEntity
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.StoryEntity
import com.tamixa.infrastructure.persistence.StoryAvatarVideoJpaRepository
import com.tamixa.infrastructure.persistence.StoryJpaRepository
import com.tamixa.infrastructure.persistence.StoryTokenUsageJpaRepository
import com.tamixa.application.analytics.StoryAnalyticsService
import com.tamixa.infrastructure.persistence.SubscriptionJpaRepository
import com.tamixa.infrastructure.persistence.VoiceProfileEntity
import com.tamixa.infrastructure.persistence.VoiceProfileJpaRepository
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.round

private val PLAN_MONTHLY_PRICE = mapOf(
    SubscriptionPlan.FREE to 0.0,
    SubscriptionPlan.PREMIUM_MONTHLY to 9.99,
    SubscriptionPlan.PREMIUM_YEARLY to 89.99 / 12,
    SubscriptionPlan.FAMILY to 14.99,
    SubscriptionPlan.VOICE_PREMIUM to 4.99
)

@Service
class AdminService(
    private val parentJpaRepository: ParentJpaRepository,
    private val childJpaRepository: ChildJpaRepository,
    private val storyJpaRepository: StoryJpaRepository,
    private val storyTokenUsageJpaRepository: StoryTokenUsageJpaRepository,
    private val storyAvatarVideoJpaRepository: StoryAvatarVideoJpaRepository,
    private val invoiceJpaRepository: InvoiceJpaRepository,
    private val adminAuditJpaRepository: AdminAuditJpaRepository,
    private val storyAnalyticsService: StoryAnalyticsService,
    private val voiceProfileJpaRepository: VoiceProfileJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val revenueMetricsService: RevenueMetricsService,
    private val revenueSnapshotPort: RevenueSnapshotPort,
    private val auditLog: AuditLogPort,
    private val registry: MeterRegistry,
    private val passwordEncoder: PasswordEncoder,
    private val storyEventPublisher: StoryEventPublisherPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getParentById(parentId: Long): ParentDetailDto? {
        val entity = parentJpaRepository.findById(parentId).orElse(null) ?: return null
        val sub = subscriptionJpaRepository.findByParent_Id(parentId)
        return entity.toDetailDto(
            status = if (entity.suspendedAt != null) "SUSPENDED" else "ACTIVE",
            plan = sub?.plan?.name ?: "FREE"
        )
    }

    @Transactional(readOnly = true)
    fun getParents(page: Int, size: Int, emailSearch: String?, statusFilter: String?): PagedResponse<ParentSummaryDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = when {
            !emailSearch.isNullOrBlank() && statusFilter == "SUSPENDED" ->
                parentJpaRepository.findByEmailContainingIgnoreCaseAndSuspendedAtIsNotNull(emailSearch.trim(), pageable)
            !emailSearch.isNullOrBlank() && statusFilter == "ACTIVE" ->
                parentJpaRepository.findByEmailContainingIgnoreCaseAndSuspendedAtIsNull(emailSearch.trim(), pageable)
            !emailSearch.isNullOrBlank() ->
                parentJpaRepository.findByEmailContainingIgnoreCase(emailSearch.trim(), pageable)
            statusFilter == "SUSPENDED" -> parentJpaRepository.findBySuspendedAtIsNotNull(pageable)
            statusFilter == "ACTIVE" -> parentJpaRepository.findBySuspendedAtIsNull(pageable)
            else -> parentJpaRepository.findAll(pageable)
        }
        val parentIds = slice.content.map { it.id }
        val subsByParent = subscriptionJpaRepository.findByParent_IdIn(parentIds).associateBy { it.parent.id }
        val content = slice.content.map { entity ->
            val sub = subsByParent[entity.id]
            entity.toSummaryDto(
                status = if (entity.suspendedAt != null) "SUSPENDED" else "ACTIVE",
                plan = sub?.plan?.name ?: "FREE"
            )
        }
        return PagedResponse(
            content = content,
            page = slice.number,
            size = slice.size,
            totalElements = slice.totalElements,
            totalPages = slice.totalPages,
            first = slice.isFirst,
            last = slice.isLast
        )
    }

    @Transactional(readOnly = true)
    fun getChildren(page: Int, size: Int, parentId: Long?): PagedResponse<ChildSummaryDto> {
        // Child feature removed; return empty for backward compatibility.
        return PagedResponse(
            content = emptyList(),
            page = 0,
            size = size.coerceIn(1, 100),
            totalElements = 0L,
            totalPages = 0,
            first = true,
            last = true
        )
    }

    @Transactional(readOnly = true)
    fun getStoryById(storyId: Long): StoryDetailDto? {
        val story = storyJpaRepository.findById(storyId).orElse(null) ?: return null
        return story.toDetailDto()
    }

    @Transactional(readOnly = true)
    fun getStories(page: Int, size: Int, status: StoryStatus?, theme: String?): PagedResponse<StorySummaryDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = when {
            status != null && !theme.isNullOrBlank() ->
                storyJpaRepository.findByStatusAndThemeContainingIgnoreCase(status, theme.trim(), pageable)
            status != null ->
                storyJpaRepository.findByStatus(status, pageable)
            !theme.isNullOrBlank() ->
                storyJpaRepository.findByThemeContainingIgnoreCase(theme.trim(), pageable)
            else ->
                storyJpaRepository.findAll(pageable)
        }
        val content = slice.content.map { it.toSummaryDto() }
        return PagedResponse(
            content = content,
            page = slice.number,
            size = slice.size,
            totalElements = slice.totalElements,
            totalPages = slice.totalPages,
            first = slice.isFirst,
            last = slice.isLast
        )
    }

    @Transactional(readOnly = true)
    fun getVoiceUploadLogs(page: Int, size: Int): PagedResponse<VoiceUploadLogDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = voiceProfileJpaRepository.findAll(pageable)
        val content = slice.content.map { it.toLogDto() }
        return PagedResponse(
            content = content,
            page = slice.number,
            size = slice.size,
            totalElements = slice.totalElements,
            totalPages = slice.totalPages,
            first = slice.isFirst,
            last = slice.isLast
        )
    }

    @Transactional(readOnly = true)
    fun getSubscriptionStatus(page: Int, size: Int): PagedResponse<SubscriptionStatusDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val parents = parentJpaRepository.findAll(pageable)
        val parentIds = parents.content.map { it.id }
        val subsByParent = subscriptionJpaRepository.findByParent_IdIn(parentIds).associateBy { it.parent.id }
        val content = parents.content.map { entity ->
            val sub = subsByParent[entity.id]
            SubscriptionStatusDto(
                parentId = entity.id,
                email = entity.email,
                status = sub?.status?.name ?: "none",
                plan = sub?.plan?.name ?: "FREE"
            )
        }
        return PagedResponse(
            content = content,
            page = parents.number,
            size = parents.size,
            totalElements = parents.totalElements,
            totalPages = parents.totalPages,
            first = parents.isFirst,
            last = parents.isLast
        )
    }

    fun getHealth(): HealthDto {
        val timer = registry.find("story_generation_latency").timer()
        val dbHealth = try {
            parentJpaRepository.count()
            "UP"
        } catch (e: Exception) {
            log.error("Health check: database unreachable", e)
            "DOWN"
        }
        val components = mapOf(
            "db" to com.tamixa.api.admin.dto.ComponentHealth(dbHealth, mapOf("database" to "postgresql")),
            "storyGeneration" to com.tamixa.api.admin.dto.ComponentHealth(
                "UP",
                mapOf("count" to (timer?.count() ?: 0L))
            )
        )
        return HealthDto(status = dbHealth, components = components)
    }

    fun getRevenueMetrics(month: java.time.YearMonth?): Map<String, Any> {
        val ym = month ?: java.time.YearMonth.now()
        val revenue = revenueMetricsService.revenueForMonth(ym)
        return mapOf(
            "month" to ym.toString(),
            "revenue" to revenue,
            "currency" to "INR"
        )
    }

    @Transactional(readOnly = true)
    fun getSubscriptionMetrics(): SubscriptionMetricsDto {
        val active = subscriptionJpaRepository.countByStatus(SubscriptionStatus.ACTIVE).toInt()
        val trial = subscriptionJpaRepository.countByStatus(SubscriptionStatus.TRIAL).toInt()
        val planDistribution = SubscriptionPlan.entries.associate { plan ->
            plan.name to subscriptionJpaRepository.countByPlanAndStatus(plan, SubscriptionStatus.ACTIVE).toInt()
        }
        val mrr = revenueMetricsService.revenueForMonth(java.time.YearMonth.now())
        val snapshot = revenueSnapshotPort.findByDate(java.time.LocalDate.now())
        return SubscriptionMetricsDto(
            activeSubscriptions = active,
            trialCount = trial,
            planDistribution = planDistribution,
            mrr = mrr,
            trialConversionRate = snapshot?.trialConversionRate,
            churnRate = snapshot?.churnRate
        )
    }

    @Transactional(readOnly = true)
    fun getRevenueTable(page: Int, size: Int, search: String?, planFilter: String?): PagedResponse<RevenueRowDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val planEnum = planFilter?.let { runCatching { SubscriptionPlan.valueOf(it) }.getOrNull() }

        if (planEnum != null) {
            val subsSlice = subscriptionJpaRepository.findByPlan(planEnum, pageable)
            val content = subsSlice.content.map { sub ->
                val parent = sub.parent
                val monthlyPayment = PLAN_MONTHLY_PRICE[sub.plan] ?: 0.0
                RevenueRowDto(
                    parentId = parent.id,
                    email = parent.email,
                    plan = sub.plan.name,
                    subscriptionState = sub.status.name,
                    monthlyPayment = monthlyPayment,
                    createdAt = parent.createdAt.toString()
                )
            }
            return PagedResponse(
                content = content,
                page = subsSlice.number,
                size = subsSlice.size,
                totalElements = subsSlice.totalElements,
                totalPages = subsSlice.totalPages,
                first = subsSlice.isFirst,
                last = subsSlice.isLast
            )
        }

        val parentsSlice = if (!search.isNullOrBlank()) {
            parentJpaRepository.findByEmailContainingIgnoreCase(search.trim(), pageable)
        } else {
            parentJpaRepository.findAll(pageable)
        }
        val parentIds = parentsSlice.content.map { it.id }
        val subsByParent = subscriptionJpaRepository.findByParent_IdIn(parentIds).associateBy { it.parent.id }
        val content = parentsSlice.content.map { parent ->
            val sub = subsByParent[parent.id]
            val plan = sub?.plan ?: SubscriptionPlan.FREE
            val monthlyPayment = PLAN_MONTHLY_PRICE[plan] ?: 0.0
            RevenueRowDto(
                parentId = parent.id,
                email = parent.email,
                plan = plan.name,
                subscriptionState = sub?.status?.name ?: "NONE",
                monthlyPayment = monthlyPayment,
                createdAt = parent.createdAt.toString()
            )
        }
        return PagedResponse(
            content = content,
            page = parentsSlice.number,
            size = parentsSlice.size,
            totalElements = parentsSlice.totalElements,
            totalPages = parentsSlice.totalPages,
            first = parentsSlice.isFirst,
            last = parentsSlice.isLast
        )
    }

    fun getRetentionMetrics(days: Int): RetentionMetricsDto =
        storyAnalyticsService.getRetentionMetrics(days.coerceIn(1, 365))

    fun getCompletionMetrics(days: Int): CompletionMetricsDto =
        storyAnalyticsService.getCompletionMetrics(days.coerceIn(1, 365))

    fun getAiMetrics(): AiMetricsDto {
        fun counterCount(name: String): Long = (registry.find(name).counter()?.count() ?: 0.0).toLong()
        val storyTimer = registry.find("story_generation_latency").timer()
        val voiceTimer = registry.find("voice.processing.latency").timer()
        val cacheHits = registry.find("story.cache.hits").counter()?.count() ?: 0.0
        val cacheMisses = registry.find("story.cache.misses").counter()?.count() ?: 0.0
        val openaiTokensStory = registry.find("story.openai.tokens.total").counter()?.count()?.toLong()
        val narrationTokens = counterCount("narration_ai_tokens_total")
        val openaiTokens = (openaiTokensStory ?: 0L) + narrationTokens
        val googleTtsRequests = counterCount("ai.google_tts.requests")
        val googleTtsChars = counterCount("ai.google_tts.characters")
        val narrationTtsChars = counterCount("narration_tts_characters_total")
        val googleTtsCharsEffective = if (googleTtsChars > 0) googleTtsChars else narrationTtsChars
        val apiBreakdown = buildList {
            add(ApiUsageDto("openai_llm", "OpenAI (story/rewrite)", storyTimer?.count() ?: 0L, if (openaiTokens > 0) openaiTokens else null, null))
            add(ApiUsageDto("google_tts", "Google Cloud TTS", googleTtsRequests, googleTtsCharsEffective.takeIf { it > 0L }, null))
            add(ApiUsageDto("openai_tts", "OpenAI TTS", counterCount("ai.openai_tts.requests"), counterCount("ai.openai_tts.characters").takeIf { it > 0L }, null))
            add(ApiUsageDto("heygen_avatar", "HeyGen (avatar video)", counterCount("ai.heygen_avatar.requests"), null, null))
            add(ApiUsageDto("replicate_avatar", "Replicate SadTalker (avatar)", counterCount("ai.replicate_avatar.requests"), null, null))
            add(ApiUsageDto("did_avatar", "D-ID (avatar)", counterCount("ai.did_avatar.requests"), null, null))
            add(ApiUsageDto("gooey_avatar", "Gooey.AI (avatar)", counterCount("ai.gooey_avatar.requests"), null, null))
        }
        val storyBreakdown = getStoryWiseAiMetrics(limit = 100)
        return AiMetricsDto(
            storyGenerationsTotal = storyTimer?.count() ?: 0L,
            cacheHits = cacheHits.toLong(),
            cacheMisses = cacheMisses.toLong(),
            voiceProcessingCount = voiceTimer?.count() ?: 0L,
            openaiTokensUsed = openaiTokens,
            apiBreakdown = apiBreakdown,
            storyBreakdown = storyBreakdown
        )
    }

    private val USD_TO_INR = 93.0
    private fun getStoryWiseAiMetrics(limit: Int): List<StoryAiUsageDto> {
        val tokenRows = try {
            storyTokenUsageJpaRepository.sumTokensByStoryId()
        } catch (e: Exception) {
            log.warn("AI metrics: failed to load token usage: {}", e.message, e)
            emptyList()
        }
        val avatarRows = try {
            storyAvatarVideoJpaRepository.countByStoryId()
        } catch (e: Exception) {
            log.warn("AI metrics: failed to load avatar counts: {}", e.message, e)
            emptyList()
        }
        val avatarByStory = avatarRows.mapNotNull { row ->
            if (row.size >= 2 && row[0] is Number && row[1] is Number) (row[0] as Number).toLong() to (row[1] as Number).toLong()
            else null
        }.toMap()

        // Batch-fetch all story titles in one query to avoid N+1
        val storyIds = tokenRows.mapNotNull { row ->
            if (row.isNotEmpty() && row[0] is Number) (row[0] as Number).toLong() else null
        }
        val titlesByStoryId = if (storyIds.isNotEmpty()) {
            storyJpaRepository.findTitlesByIds(storyIds)
                .mapNotNull { row ->
                    if (row.size >= 2 && row[0] is Number) (row[0] as Number).toLong() to row[1]?.toString()
                    else null
                }.toMap()
        } else emptyMap()

        val list = tokenRows.mapNotNull { row ->
            if (row.size >= 2 && row[0] is Number && row[1] is Number) {
                val storyId = (row[0] as Number).toLong()
                val totalTokens = (row[1] as Number).toLong()
                val avatarCount = avatarByStory[storyId] ?: 0L
                val title = titlesByStoryId[storyId]
                val costUsd = (totalTokens / 1_000_000.0) * 0.001 + avatarCount * 0.10
                val costInr = costUsd * USD_TO_INR
                StoryAiUsageDto(storyId = storyId, title = title, totalTokens = totalTokens, avatarVideoCount = avatarCount, costInr = costInr)
            } else null
        }.sortedByDescending { it.totalTokens }.take(limit)
        return list
    }

    private fun recordAdminAction(adminEmail: String, action: String, resourceType: String, resourceId: String?, details: String?) {
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY)
        auditLog.logAdminAction(adminEmail, action, resourceType, resourceId, details, traceId)
        adminAuditJpaRepository.save(
            AdminAuditEntity(
                adminEmail = adminEmail,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                details = details,
                traceId = traceId
            )
        )
    }

    @Transactional
    fun flagStory(adminEmail: String, storyId: Long, reason: String?) {
        val story = storyJpaRepository.findById(storyId).orElse(null)
            ?: throw StoryNotFoundException(storyId)
        story.status = StoryStatus.FLAGGED
        storyJpaRepository.save(story)
        recordAdminAction(adminEmail, "flag_story", "story", storyId.toString(), reason ?: "Flagged for moderation")
    }

    @Transactional
    fun suspendParent(adminEmail: String, parentId: Long) {
        val parent = parentJpaRepository.findById(parentId).orElse(null)
            ?: throw AdminParentNotFoundException(parentId)
        parent.suspendedAt = java.time.Instant.now()
        parentJpaRepository.save(parent)
        recordAdminAction(adminEmail, "suspend_parent", "parent", parentId.toString(), "Account suspended by admin")
    }

    @Transactional
    fun unsuspendParent(adminEmail: String, parentId: Long) {
        val parent = parentJpaRepository.findById(parentId).orElse(null)
            ?: throw AdminParentNotFoundException(parentId)
        parent.suspendedAt = null
        parentJpaRepository.save(parent)
        recordAdminAction(adminEmail, "unsuspend_parent", "parent", parentId.toString(), "Account unsuspended by admin")
    }

    @Transactional
    fun createParent(adminEmail: String, request: CreateParentRequest): ParentDetailDto {
        if (parentJpaRepository.existsByEmail(request.email.trim())) {
            throw EmailAlreadyExistsException(request.email)
        }
        val phoneToSet = request.phone?.trim()?.takeIf { it.isNotBlank() }?.let { normalizePhone(it) }
        if (phoneToSet != null && parentJpaRepository.existsByPhone(phoneToSet)) {
            throw PhoneAlreadyInUseException(phoneToSet)
        }
        val role = request.role?.trim()?.uppercase()?.let { Role.valueOf(it) } ?: Role.PARENT
        val hash = passwordEncoder.encode(request.password)
        val entity = ParentEntity(
            id = 0,
            email = request.email.trim(),
            passwordHash = hash,
            role = role,
            createdAt = Instant.now(),
            phone = phoneToSet,
            suspendedAt = null
        )
        val saved = parentJpaRepository.save(entity)
        recordAdminAction(adminEmail, "create_parent", "parent", saved.id.toString(), "Created parent ${PiiMask.maskEmail(saved.email)}")
        val sub = subscriptionJpaRepository.findByParent_Id(saved.id)
        return saved.toDetailDto(status = "ACTIVE", plan = sub?.plan?.name ?: "FREE")
    }

    @Transactional
    fun updateParent(adminEmail: String, parentId: Long, request: UpdateParentRequest): ParentDetailDto {
        val parent = parentJpaRepository.findById(parentId).orElse(null)
            ?: throw AdminParentNotFoundException(parentId)
        request.email?.trim()?.takeIf { it.isNotBlank() }?.let { email ->
            parentJpaRepository.findByEmail(email)?.let { existing ->
                if (existing.id != parentId) throw EmailAlreadyExistsException(email)
            }
            parent.email = email
        }
        request.phone?.trim()?.let { raw ->
            if (raw.isBlank()) {
                parent.phone = null
            } else {
                val normalized = normalizePhone(raw)
                if (normalized != null) {
                    val existingWithPhone = parentJpaRepository.findByPhone(normalized)
                    if (existingWithPhone != null && existingWithPhone.id != parentId) {
                        throw PhoneAlreadyInUseException(normalized)
                    }
                    parent.phone = normalized
                } else {
                    parent.phone = null
                }
            }
        }
        request.role?.trim()?.uppercase()?.let { parent.role = Role.valueOf(it) }
        val saved = parentJpaRepository.save(parent)
        recordAdminAction(adminEmail, "update_parent", "parent", parentId.toString(), "Updated parent ${PiiMask.maskEmail(saved.email)}")
        val sub = subscriptionJpaRepository.findByParent_Id(saved.id)
        return saved.toDetailDto(
            status = if (saved.suspendedAt != null) "SUSPENDED" else "ACTIVE",
            plan = sub?.plan?.name ?: "FREE"
        )
    }

    @Transactional
    fun deleteParent(adminEmail: String, parentId: Long) {
        val parent = parentJpaRepository.findById(parentId).orElse(null)
            ?: throw AdminParentNotFoundException(parentId)
        childJpaRepository.deleteByParent_Id(parentId)
        subscriptionJpaRepository.findByParent_Id(parentId)?.let { subscriptionJpaRepository.delete(it) }
        parentJpaRepository.delete(parent)
        recordAdminAction(adminEmail, "delete_parent", "parent", parentId.toString(), "Deleted parent ${PiiMask.maskEmail(parent.email)}")
    }

    @Transactional
    fun approveStory(adminEmail: String, storyId: Long) {
        val story = storyJpaRepository.findById(storyId).orElse(null)
            ?: throw StoryNotFoundException(storyId)
        if (story.status == StoryStatus.PENDING_REVIEW) {
            val content = story.content
            story.status = StoryStatus.PENDING
            storyJpaRepository.save(story)
            recordAdminAction(
                adminEmail,
                "approve_story",
                "story",
                storyId.toString(),
                "Human review passed; narration pipeline scheduled",
            )
            val id = storyId
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    object : TransactionSynchronization {
                        override fun afterCommit() {
                            try {
                                storyEventPublisher.publishStoryCreated(id, content)
                            } catch (e: Exception) {
                                log.error("publishStoryCreated after human review failed storyId={}", id, e)
                            }
                        }
                    },
                )
            } else {
                storyEventPublisher.publishStoryCreated(id, content)
            }
            return
        }
        story.status = StoryStatus.READY
        storyJpaRepository.save(story)
        recordAdminAction(adminEmail, "approve_story", "story", storyId.toString(), "Approved by admin")
    }

    @Transactional
    fun rejectStory(adminEmail: String, storyId: Long) {
        val story = storyJpaRepository.findById(storyId).orElse(null)
            ?: throw StoryNotFoundException(storyId)
        story.status = StoryStatus.FAILED
        storyJpaRepository.save(story)
        recordAdminAction(adminEmail, "reject_story", "story", storyId.toString(), "Rejected by admin")
    }

    /** Stub: Kafka events are observed via logs and metrics; no in-app event store. */
    fun getKafkaEvents(page: Int, size: Int): PagedResponse<Map<String, Any>> {
        return PagedResponse(
            content = emptyList(),
            page = page,
            size = size,
            totalElements = 0L,
            totalPages = 0,
            first = true,
            last = true
        )
    }

    @Transactional(readOnly = true)
    fun getAuditTrail(page: Int, size: Int): PagedResponse<Map<String, Any>> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = adminAuditJpaRepository.findAllByOrderByCreatedAtDesc(pageable)
        val content = slice.content.map { e ->
            mapOf(
                "id" to e.id,
                "adminEmail" to e.adminEmail,
                "action" to e.action,
                "resourceType" to e.resourceType,
                "resourceId" to (e.resourceId ?: ""),
                "details" to (e.details ?: ""),
                "createdAt" to e.createdAt.toString()
            )
        }
        return PagedResponse(
            content = content,
            page = slice.number,
            size = slice.size,
            totalElements = slice.totalElements,
            totalPages = slice.totalPages,
            first = slice.isFirst,
            last = slice.isLast
        )
    }

    @Transactional(readOnly = true)
    fun getStoryUsagePerDay(days: Int): List<StoryUsagePointDto> {
        val since = java.time.Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        val rows = storyJpaRepository.countByDaySince(since)
        return rows.map { arr ->
            val day = arr[0] // java.sql.Date or LocalDate
            val cnt = (arr[1] as? Number)?.toLong() ?: 0L
            val dateStr = when (day) {
                is java.sql.Date -> day.toLocalDate().toString()
                is java.time.LocalDate -> day.toString()
                else -> day.toString()
            }
            StoryUsagePointDto(date = dateStr, count = cnt)
        }
    }

    @Transactional(readOnly = true)
    fun getStoryLengthProfile(days: Int): Map<String, Any> {
        val safeDays = days.coerceIn(1, 90)
        val since = java.time.Instant.now().minus(safeDays.toLong(), ChronoUnit.DAYS)
        val row = storyJpaRepository.storyLengthProfileSince(since)
        val totalStories = (row?.getOrNull(0) as? Number)?.toLong() ?: 0L
        val avgWordCount = (row?.getOrNull(1) as? Number)?.toDouble() ?: 0.0
        val avgReadingTimeMinutes = (row?.getOrNull(2) as? Number)?.toDouble() ?: 0.0
        val expectedMinutesByWords = avgWordCount / 120.0
        fun round2(value: Double): Double = round(value * 100.0) / 100.0
        return mapOf(
            "windowDays" to safeDays,
            "totalStories" to totalStories,
            "avgWordCount" to round2(avgWordCount),
            "avgReadingTimeMinutes" to round2(avgReadingTimeMinutes),
            "avgExpectedMinutesByWords" to round2(expectedMinutesByWords),
            "wpmAssumption" to 120
        )
    }

    @Transactional(readOnly = true)
    fun getInvoices(page: Int, size: Int): PagedResponse<AdminInvoiceDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = invoiceJpaRepository.findAllByOrderByCreatedAtDesc(pageable)
        val parentIds = slice.content.map { it.parent.id }.distinct()
        val parents = parentJpaRepository.findAllById(parentIds).associateBy { it.id }
        val subsByParent = subscriptionJpaRepository.findByParent_IdIn(parentIds).associateBy { it.parent.id }
        val content = slice.content.map { inv ->
            val parent = parents[inv.parent.id]!!
            val sub = subsByParent[inv.parent.id]
            AdminInvoiceDto(
                id = inv.id,
                providerInvoiceId = inv.providerInvoiceId,
                parentId = inv.parent.id,
                email = parent.email,
                amount = inv.amountMinor / 100.0,
                currency = inv.currency,
                status = inv.status.name,
                plan = sub?.plan?.name ?: "FREE",
                dueDate = inv.createdAt.toString().take(10),
                paidAt = inv.paidAt
            )
        }
        return PagedResponse(
            content = content,
            page = slice.number,
            size = slice.size,
            totalElements = slice.totalElements,
            totalPages = slice.totalPages,
            first = slice.isFirst,
            last = slice.isLast
        )
    }

    @Transactional
    fun refundInvoice(adminEmail: String, invoiceId: Long) {
        val invoice = invoiceJpaRepository.findById(invoiceId).orElse(null)
            ?: throw InvoiceNotFoundException(invoiceId)
        if (invoice.status != InvoiceStatus.PAID) {
            throw InvoiceNotRefundableException("Only PAID invoices can be refunded")
        }
        invoice.status = InvoiceStatus.REFUNDED
        invoiceJpaRepository.save(invoice)
        recordAdminAction(adminEmail, "refund_invoice", "invoice", invoiceId.toString(), "Invoice refunded by admin")
    }

    /** Records an admin action (e.g. from controller). Use for delete/other ops not in AdminService. */
    @Transactional
    fun recordAdminAuditAction(adminEmail: String, action: String, resourceType: String, resourceId: String?, details: String?) {
        recordAdminAction(adminEmail, action, resourceType, resourceId, details)
    }

    private val ADMIN_ROLES = listOf(Role.SUPER_ADMIN, Role.ADMIN, Role.REVENUE_ANALYST, Role.CONTENT_MANAGER, Role.SUPPORT)

    @Transactional(readOnly = true)
    fun getAdminUsers(page: Int, size: Int): PagedResponse<AdminUserDto> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = parentJpaRepository.findByRoleIn(ADMIN_ROLES, pageable)
        val content = slice.content.map { AdminUserDto(id = it.id, email = it.email, role = it.role.name, createdAt = it.createdAt) }
        return PagedResponse(
            content = content,
            page = slice.number,
            size = slice.size,
            totalElements = slice.totalElements,
            totalPages = slice.totalPages,
            first = slice.isFirst,
            last = slice.isLast
        )
    }

    @Transactional
    fun addAdminUser(adminEmail: String, request: AddAdminUserRequest): AdminUserDto {
        val parent = parentJpaRepository.findById(request.parentId).orElseThrow { AdminParentNotFoundException(request.parentId) }
        val role = Role.valueOf(request.role)
        if (parent.role in ADMIN_ROLES) {
            throw IllegalArgumentException("User ${PiiMask.maskEmail(parent.email)} already has admin role ${parent.role}. Use update role instead.")
        }
        parent.role = role
        parentJpaRepository.save(parent)
        recordAdminAction(adminEmail, "add_admin_user", "parent", parent.id.toString(), "Added ${PiiMask.maskEmail(parent.email)} as $role")
        return AdminUserDto(id = parent.id, email = parent.email, role = parent.role.name, createdAt = parent.createdAt)
    }

    @Transactional
    fun updateAdminRole(adminEmail: String, userId: Long, role: Role): AdminUserDto {
        val parent = parentJpaRepository.findById(userId).orElseThrow { AdminParentNotFoundException(userId) }
        if (parent.role !in ADMIN_ROLES && role in ADMIN_ROLES) {
            recordAdminAction(adminEmail, "add_admin_user", "parent", userId.toString(), "Promoted ${PiiMask.maskEmail(parent.email)} to $role")
        } else if (parent.role in ADMIN_ROLES && role == Role.PARENT) {
            recordAdminAction(adminEmail, "revoke_admin", "parent", userId.toString(), "Revoked admin access for ${PiiMask.maskEmail(parent.email)}")
        } else {
            recordAdminAction(adminEmail, "update_admin_role", "parent", userId.toString(), "Updated ${PiiMask.maskEmail(parent.email)} role to $role")
        }
        parent.role = role
        parentJpaRepository.save(parent)
        return AdminUserDto(id = parent.id, email = parent.email, role = parent.role.name, createdAt = parent.createdAt)
    }

    /** Normalize phone to canonical form for storage and uniqueness checks. Matches OtpService format. */
    private fun normalizePhone(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        if (digits.length !in 10..15) return null
        return if (digits.length == 10 && digits[0] in '6'..'9') "+91$digits" else "+$digits"
    }
}

private fun ParentEntity.toSummaryDto(status: String = if (suspendedAt != null) "SUSPENDED" else "ACTIVE", plan: String = "FREE") = ParentSummaryDto(
    id = id,
    email = email,
    role = role.name,
    status = status,
    plan = plan,
    createdAt = createdAt
)

private fun ParentEntity.toDetailDto(status: String, plan: String) = ParentDetailDto(
    id = id,
    email = email,
    role = role.name,
    status = status,
    plan = plan,
    phone = phone,
    createdAt = createdAt,
    suspendedAt = suspendedAt
)

private fun StoryEntity.toDetailDto() = StoryDetailDto(
    id = id,
    parentId = parent.id,
    childId = child?.id,
    theme = theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    status = status.name,
    content = content,
    createdAt = createdAt
)

private fun StoryEntity.toSummaryDto() = StorySummaryDto(
    id = id,
    parentId = parent.id,
    childId = child?.id,
    theme = theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    status = status.name,
    safetyScore = safetyScore,
    createdAt = createdAt
)

private fun VoiceProfileEntity.toLogDto() = VoiceUploadLogDto(
    id = id,
    parentId = parent.id,
    parentEmail = parent.email,
    createdAt = createdAt
)

class StoryNotFoundException(id: Long) : RuntimeException("Story not found: $id")

class AdminParentNotFoundException(id: Long) : RuntimeException("Parent not found: $id")

class ParentHasDependentsException(message: String) : RuntimeException(message)

class InvoiceNotFoundException(id: Long) : RuntimeException("Invoice not found: $id")

class InvoiceNotRefundableException(message: String) : RuntimeException(message)
