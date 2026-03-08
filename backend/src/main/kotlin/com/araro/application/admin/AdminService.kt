package com.araro.application.admin

import com.araro.api.admin.dto.AddAdminUserRequest
import com.araro.api.admin.dto.AdminInvoiceDto
import com.araro.api.admin.dto.AdminUserDto
import com.araro.api.admin.dto.AiMetricsDto
import com.araro.application.analytics.CompletionMetricsDto
import com.araro.application.analytics.RetentionMetricsDto
import com.araro.api.admin.dto.ChildSummaryDto
import com.araro.api.admin.dto.HealthDto
import com.araro.api.admin.dto.PagedResponse
import com.araro.api.admin.dto.ParentSummaryDto
import com.araro.api.admin.dto.RevenueRowDto
import com.araro.api.admin.dto.StoryDetailDto
import com.araro.api.admin.dto.StorySummaryDto
import com.araro.api.admin.dto.StoryUsagePointDto
import com.araro.api.admin.dto.SubscriptionMetricsDto
import com.araro.api.admin.dto.SubscriptionStatusDto
import com.araro.api.admin.dto.VoiceUploadLogDto
import com.araro.api.config.RequestTracingFilter
import com.araro.application.port.AuditLogPort
import com.araro.application.subscription.RevenueMetricsService
import com.araro.application.port.RevenueSnapshotPort
import com.araro.domain.InvoiceStatus
import com.araro.domain.Role
import com.araro.domain.StoryStatus
import com.araro.domain.SubscriptionPlan
import com.araro.domain.SubscriptionStatus
import com.araro.infrastructure.persistence.AdminAuditEntity
import com.araro.infrastructure.persistence.AdminAuditJpaRepository
import com.araro.infrastructure.persistence.ChildEntity
import com.araro.infrastructure.persistence.ChildJpaRepository
import com.araro.infrastructure.persistence.InvoiceEntity
import com.araro.infrastructure.persistence.InvoiceJpaRepository
import com.araro.infrastructure.persistence.ParentEntity
import com.araro.infrastructure.persistence.ParentJpaRepository
import com.araro.infrastructure.persistence.StoryEntity
import com.araro.infrastructure.persistence.StoryJpaRepository
import com.araro.application.analytics.StoryAnalyticsService
import com.araro.infrastructure.persistence.SubscriptionJpaRepository
import com.araro.infrastructure.persistence.VoiceProfileEntity
import com.araro.infrastructure.persistence.VoiceProfileJpaRepository
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit

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
    private val invoiceJpaRepository: InvoiceJpaRepository,
    private val adminAuditJpaRepository: AdminAuditJpaRepository,
    private val storyAnalyticsService: StoryAnalyticsService,
    private val voiceProfileJpaRepository: VoiceProfileJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val revenueMetricsService: RevenueMetricsService,
    private val revenueSnapshotPort: RevenueSnapshotPort,
    private val auditLog: AuditLogPort,
    private val registry: MeterRegistry
) {

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
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val slice = if (parentId != null) {
            childJpaRepository.findByParent_Id(parentId, pageable)
        } else {
            childJpaRepository.findAll(pageable)
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
        val timer = registry.find("story.generation.latency").timer()
        val dbHealth = try {
            parentJpaRepository.count()
            "UP"
        } catch (e: Exception) {
            "DOWN"
        }
        val components = mapOf(
            "db" to com.araro.api.admin.dto.ComponentHealth(dbHealth, mapOf("database" to "postgresql")),
            "storyGeneration" to com.araro.api.admin.dto.ComponentHealth(
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
            "currency" to "USD"
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
        val storyTimer = registry.find("story.generation.latency").timer()
        val voiceTimer = registry.find("voice.processing.latency").timer()
        val cacheHits = registry.find("story.cache.hits").counter()?.count() ?: 0.0
        val cacheMisses = registry.find("story.cache.misses").counter()?.count() ?: 0.0
        return AiMetricsDto(
            storyGenerationsTotal = storyTimer?.count() ?: 0L,
            cacheHits = cacheHits.toLong(),
            cacheMisses = cacheMisses.toLong(),
            voiceProcessingCount = voiceTimer?.count() ?: 0L,
            openaiTokensUsed = null
        )
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
    fun approveStory(adminEmail: String, storyId: Long) {
        val story = storyJpaRepository.findById(storyId).orElse(null)
            ?: throw StoryNotFoundException(storyId)
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
            throw IllegalArgumentException("User ${parent.email} already has admin role ${parent.role}. Use update role instead.")
        }
        parent.role = role
        parentJpaRepository.save(parent)
        recordAdminAction(adminEmail, "add_admin_user", "parent", parent.id.toString(), "Added ${parent.email} as $role")
        return AdminUserDto(id = parent.id, email = parent.email, role = parent.role.name, createdAt = parent.createdAt)
    }

    @Transactional
    fun updateAdminRole(adminEmail: String, userId: Long, role: Role): AdminUserDto {
        val parent = parentJpaRepository.findById(userId).orElseThrow { AdminParentNotFoundException(userId) }
        if (parent.role !in ADMIN_ROLES && role in ADMIN_ROLES) {
            recordAdminAction(adminEmail, "add_admin_user", "parent", userId.toString(), "Promoted ${parent.email} to $role")
        } else if (parent.role in ADMIN_ROLES && role == Role.PARENT) {
            recordAdminAction(adminEmail, "revoke_admin", "parent", userId.toString(), "Revoked admin access for ${parent.email}")
        } else {
            recordAdminAction(adminEmail, "update_admin_role", "parent", userId.toString(), "Updated ${parent.email} role to $role")
        }
        parent.role = role
        parentJpaRepository.save(parent)
        return AdminUserDto(id = parent.id, email = parent.email, role = parent.role.name, createdAt = parent.createdAt)
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

private fun ChildEntity.toSummaryDto() = ChildSummaryDto(
    id = id,
    parentId = parent.id,
    parentEmail = parent.email,
    name = name,
    dateOfBirth = dateOfBirth,
    age = ChronoUnit.YEARS.between(dateOfBirth, java.time.LocalDate.now()).toInt(),
    languagePreference = languagePreference,
    createdAt = createdAt
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

class InvoiceNotFoundException(id: Long) : RuntimeException("Invoice not found: $id")

class InvoiceNotRefundableException(message: String) : RuntimeException(message)
