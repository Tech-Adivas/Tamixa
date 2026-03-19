package com.tamixa.api.subscription

import com.tamixa.api.ApiVersion
import com.tamixa.api.subscription.dto.ReferralCodeValidateResponse
import com.tamixa.api.subscription.dto.SubscriptionResponse
import com.tamixa.api.subscription.dto.UpgradeRequest
import com.tamixa.api.subscription.dto.UpgradeResponse
import com.tamixa.api.subscription.dto.UsageResponse
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.UsageTrackingPort
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.SubscriptionPlan
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("${ApiVersion.V1}/subscription")
@PreAuthorize("hasRole('PARENT')")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val parentRepository: ParentRepositoryPort,
    private val usageTrackingPort: UsageTrackingPort,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/usage")
    fun getUsage(): ResponseEntity<UsageResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.notFound().build()
        val parent = parentRepository.findByEmail(email) ?: return ResponseEntity.notFound().build()
        log.debug("Subscription getUsage parentId={}", parent.id)
        val sub = subscriptionService.getOrCreateSubscription(parent.id)
        val month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val usage = usageTrackingPort.getOrCreate(parent.id, month)
        val storiesLimit = when (sub.plan) {
            SubscriptionPlan.FREE -> appProperties.subscription.freeStoriesPerMonth
            SubscriptionPlan.STARTER -> appProperties.subscription.starterStoriesPerMonth
            else -> null  // PREMIUM_MONTHLY, PREMIUM_YEARLY, FAMILY, VOICE_PREMIUM: unlimited
        }
        return ResponseEntity.ok(
            UsageResponse(
                month = month,
                storiesUsed = usage.storiesGenerated,
                storiesLimit = storiesLimit,
                voiceUsed = usage.voiceGenerations,
                voiceLimit = appProperties.subscription.freeVoiceGenerationsPerMonth
            )
        )
    }

    @GetMapping
    fun getCurrent(): ResponseEntity<SubscriptionResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.notFound().build()
        val parent = parentRepository.findByEmail(email) ?: return ResponseEntity.notFound().build()
        log.debug("Subscription getCurrent parentId={}", parent.id)
        val sub = subscriptionService.getOrCreateSubscription(parent.id)
        return ResponseEntity.ok(
            SubscriptionResponse(
                plan = sub.plan.name,
                status = sub.status.name,
                provider = sub.provider.name,
                currentPeriodEnd = sub.currentPeriodEnd,
                trialEnd = sub.trialEnd,
                cancelAtPeriodEnd = sub.cancelAtPeriodEnd,
                maxChildren = sub.maxChildren,
                voicePremium = sub.voicePremium,
                isEntitledToUnlimitedStories = sub.isEntitledToUnlimitedStories()
            )
        )
    }

    @GetMapping("/referral-code/validate")
    fun validateReferralCode(@RequestParam code: String): ResponseEntity<ReferralCodeValidateResponse> {
        SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val info = subscriptionService.validateReferralCode(code)
        return if (info != null) {
            ResponseEntity.ok(
                ReferralCodeValidateResponse(
                    valid = true,
                    shortcode = info.shortcode,
                    shopName = info.shopName,
                    offerPercent = info.offerPercent
                )
            )
        } else {
            ResponseEntity.ok(ReferralCodeValidateResponse(valid = false))
        }
    }

    @PostMapping("/upgrade")
    fun upgrade(@jakarta.validation.Valid @RequestBody request: UpgradeRequest? = null): ResponseEntity<UpgradeResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val parent = parentRepository.findByEmail(email) ?: return ResponseEntity.notFound().build()
        log.info("Subscription upgrade requested parentId={} referralCode={}", parent.id, request?.referralCode?.take(3)?.let { "$it***" })
        val prefix = appProperties.subscription.stripe.successUrlPrefix
        val result = subscriptionService.createCheckoutSession(
            parentId = parent.id,
            successUrl = request?.successUrl ?: "$prefix/success",
            cancelUrl = request?.cancelUrl ?: "$prefix/cancel",
            referralCode = request?.referralCode
        )
        return if (result != null) {
            log.info("Subscription upgrade checkout created parentId={}", parent.id)
            ResponseEntity.ok(UpgradeResponse(checkoutUrl = result))
        } else {
            log.warn("Subscription upgrade failed parentId={} (checkout session null)", parent.id)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }
    }

    @PostMapping("/cancel")
    fun cancelAtPeriodEnd(): ResponseEntity<Unit> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.notFound().build()
        val parent = parentRepository.findByEmail(email) ?: return ResponseEntity.notFound().build()
        val sub = subscriptionService.getOrCreateSubscription(parent.id)
        log.info("Subscription cancel requested parentId={} subscriptionId={}", parent.id, sub.id)
        subscriptionService.cancelAtPeriodEnd(sub.id, parent.id)
        return ResponseEntity.ok().build()
    }
}
