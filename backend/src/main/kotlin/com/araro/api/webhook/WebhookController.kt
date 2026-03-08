package com.araro.api.webhook

import com.araro.api.ApiVersion
import com.araro.application.port.WebhookEventPort
import com.araro.application.port.WebhookEventStatus
import com.araro.application.port.WebhookIdempotencyPort
import com.araro.application.subscription.SubscriptionService
import com.araro.domain.BillingEventType
import com.araro.domain.PaymentProvider
import com.araro.domain.SubscriptionPlan
import com.araro.domain.SubscriptionStatus
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.webhook.WebhookSignatureVerifier
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest
import java.time.Instant

/**
 * Webhook endpoints for Stripe and Zoho Payments.
 * Security: signature verification required; idempotent processing by event id.
 * No client-side subscription trust: all state changes come from verified webhooks or server logic.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/webhooks")
class WebhookController(
    private val subscriptionService: SubscriptionService,
    private val signatureVerifier: WebhookSignatureVerifier,
    private val idempotency: WebhookIdempotencyPort,
    private val webhookEventPort: WebhookEventPort,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/stripe")
    fun stripeWebhook(
        request: HttpServletRequest,
        @RequestHeader("Stripe-Signature") signature: String?
    ): ResponseEntity<String> {
        val rawBody = readRawBody(request) ?: return ResponseEntity.badRequest().body("Missing body")
        val secret = appProperties.subscription.stripe.webhookSecret
        if (!appProperties.subscription.stripe.enabled || secret.isBlank()) {
            log.warn("Stripe webhooks disabled or secret not set")
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Webhooks not configured")
        }
        if (!signatureVerifier.verify(rawBody, signature, PaymentProvider.STRIPE, secret)) {
            log.warn("Stripe webhook invalid signature")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature")
        }
        val node = objectMapper.readTree(rawBody)
        val eventId = node.path("id").asText().takeIf { it.isNotBlank() }
        val type = node.path("type").asText().takeIf { it.isNotBlank() } ?: return ResponseEntity.badRequest().body("Missing type")
        log.info("Stripe webhook received type={} eventId={}", type, eventId)
        if (eventId.isNullOrBlank()) return ResponseEntity.badRequest().body("Missing event id")
        if (idempotency.alreadyProcessed("STRIPE", eventId)) {
            log.debug("Stripe webhook duplicate event_id (replay prevention): {}", eventId)
            return ResponseEntity.ok("Already processed")
        }
        var status = WebhookEventStatus.PROCESSED
        try {
            handleStripeEvent(eventId, type, node, rawBody)
        } catch (e: Exception) {
            log.error("Stripe webhook processing failed: type={} eventId={}", type, eventId, e)
            status = WebhookEventStatus.FAILED
            webhookEventPort.record("STRIPE", eventId, type, status, rawBody)
            idempotency.markProcessed("STRIPE", eventId)
            throw e
        }
        webhookEventPort.record("STRIPE", eventId, type, status, rawBody)
        idempotency.markProcessed("STRIPE", eventId)
        return ResponseEntity.ok("OK")
    }

    @PostMapping("/zoho")
    fun zohoWebhook(
        request: HttpServletRequest,
        @RequestHeader("X-Zoho-Webhook-Signature") signature: String?
    ): ResponseEntity<String> {
        val rawBody = readRawBody(request) ?: return ResponseEntity.badRequest().body("Missing body")
        val signingKey = appProperties.subscription.zoho.webhookSigningKey
        if (!appProperties.subscription.zoho.enabled || signingKey.isBlank()) {
            log.warn("Zoho webhooks disabled or signing key not set")
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Webhooks not configured")
        }
        if (!signatureVerifier.verify(rawBody, signature, PaymentProvider.ZOHO, signingKey)) {
            log.warn("Zoho webhook invalid signature")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature")
        }
        val node = objectMapper.readTree(rawBody)
        val eventId = node.path("event_id").asText().takeIf { it.isNotBlank() }
            ?: node.path("event_id").asLong(0).takeIf { it > 0 }?.toString()
            ?: java.util.UUID.randomUUID().toString()
        val eventType = node.path("event_type").asText().takeIf { it.isNotBlank() } ?: "unknown"
        log.info("Zoho webhook received eventType={} eventId={}", eventType, eventId)
        if (idempotency.alreadyProcessed("ZOHO", eventId)) {
            log.debug("Zoho webhook duplicate event_id (replay prevention): {}", eventId)
            return ResponseEntity.ok("Already processed")
        }
        var status = WebhookEventStatus.PROCESSED
        try {
            handleZohoEvent(eventId, eventType, node)
        } catch (e: Exception) {
            log.error("Zoho webhook processing failed: event={} eventId={}", eventType, eventId, e)
            status = WebhookEventStatus.FAILED
            webhookEventPort.record("ZOHO", eventId, eventType, status, rawBody)
            idempotency.markProcessed("ZOHO", eventId)
            throw e
        }
        webhookEventPort.record("ZOHO", eventId, eventType, status, rawBody)
        idempotency.markProcessed("ZOHO", eventId)
        return ResponseEntity.ok("OK")
    }

    private fun readRawBody(request: HttpServletRequest): String? {
        return try {
            request.reader.buffered().readText()
        } catch (e: Exception) {
            log.warn("Failed to read webhook body", e)
            null
        }
    }

    private fun handleStripeEvent(eventId: String, type: String, node: JsonNode, rawBody: String) {
        val data = node.path("data").path("object")
        when (type) {
            "customer.subscription.created", "customer.subscription.updated" -> handleStripeSubscription(data, type)
            "customer.subscription.deleted" -> handleStripeSubscriptionDeleted(data)
            "invoice.paid" -> handleStripeInvoicePaid(data)
            "invoice.payment_failed" -> handleStripeInvoicePaymentFailed(data)
            "payment_intent.succeeded" -> handleStripePaymentSucceeded(data)
            "charge.refunded" -> handleStripeRefund(data)
            else -> log.debug("Unhandled Stripe event type: {}", type)
        }
    }

    private fun handleStripeSubscription(data: JsonNode, type: String) {
        val subId = data.path("id").asText()
        val customerId = data.path("customer").asText().takeIf { it.isNotBlank() }
        val status = mapStripeStatus(data.path("status").asText().takeIf { it.isNotBlank() })
        val periodStart = data.path("current_period_start").asLong(0).takeIf { it > 0 }
            ?.let { Instant.ofEpochSecond(it) }
        val periodEnd = data.path("current_period_end").asLong(0).takeIf { it > 0 }
            ?.let { Instant.ofEpochSecond(it) }
        val trialEnd = data.path("trial_end").asLong(0).takeIf { it > 0 }?.let { Instant.ofEpochSecond(it) }
        val cancelAtPeriodEnd = data.path("cancel_at_period_end").asBoolean(false)
        val metadata = data.path("metadata")
        val parentIdStr = metadata.path("parent_id").asText().takeIf { it.isNotBlank() }
        val parentId = parentIdStr?.toLongOrNull()
        if (parentId == null) {
            log.warn("Stripe subscription missing parent_id in metadata: {}", subId)
            return
        }
        val plan = metadata.path("plan").asText("PREMIUM_MONTHLY").let { mapStripePlan(it) }
        val maxChildren = metadata.path("max_children").asInt(1)
        val voicePremium = metadata.path("voice_premium").asBoolean(false)
        subscriptionService.upsertFromProvider(
            parentId = parentId,
            plan = plan,
            status = status,
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = subId,
            externalCustomerId = customerId,
            currentPeriodStart = periodStart,
            currentPeriodEnd = periodEnd,
            trialEnd = trialEnd,
            cancelAtPeriodEnd = cancelAtPeriodEnd,
            maxChildren = maxChildren,
            voicePremium = voicePremium
        )
    }

    private fun handleStripeSubscriptionDeleted(data: JsonNode) {
        val subId = data.path("id").asText()
        val sub = subscriptionService.findByProviderAndExternalId(PaymentProvider.STRIPE, subId) ?: return
        subscriptionService.transitionState(sub.id, SubscriptionStatus.EXPIRED, "stripe.deleted", null, PaymentProvider.STRIPE)
    }

    private fun handleStripeInvoicePaid(data: JsonNode) {
        val subscriptionId = data.path("subscription").asText().takeIf { it.isNotBlank() }
        val amountPaid = data.path("amount_paid").asLong(0)
        val currency = data.path("currency").asText().takeIf { it.isNotBlank() }
        val invoiceId = data.path("id").asText().takeIf { it.isNotBlank() }
        val sub = subscriptionId?.let { subscriptionService.findByProviderAndExternalId(PaymentProvider.STRIPE, it) }
        val parentId = sub?.parentId ?: return
        subscriptionService.recordPaymentSucceeded(
            parentId = parentId,
            subscriptionId = sub.id,
            provider = PaymentProvider.STRIPE,
            externalId = invoiceId,
            amountMinor = amountPaid,
            currency = currency,
            invoiceId = invoiceId
        )
    }

    private fun handleStripeInvoicePaymentFailed(data: JsonNode) {
        val subscriptionId = data.path("subscription").asText().takeIf { it.isNotBlank() }
        val sub = subscriptionId?.let { subscriptionService.findByProviderAndExternalId(PaymentProvider.STRIPE, it) }
        val parentId = sub?.parentId ?: return
        subscriptionService.recordPaymentFailed(
            parentId = parentId,
            subscriptionId = sub.id,
            provider = PaymentProvider.STRIPE,
            externalId = data.path("id").asText().takeIf { it.isNotBlank() },
            details = data.path("last_payment_error").path("message").asText().takeIf { it.isNotBlank() }
        )
    }

    private fun handleStripePaymentSucceeded(data: JsonNode) {
        val amount = data.path("amount").asLong(0)
        val currency = data.path("currency").asText().takeIf { it.isNotBlank() }
        val metadata = data.path("metadata")
        val parentIdStr = metadata.path("parent_id").asText().takeIf { it.isNotBlank() } ?: return
        val parentId = parentIdStr.toLongOrNull() ?: return
        subscriptionService.recordPaymentSucceeded(
            parentId = parentId,
            subscriptionId = null,
            provider = PaymentProvider.STRIPE,
            externalId = data.path("id").asText().takeIf { it.isNotBlank() },
            amountMinor = amount,
            currency = currency,
            invoiceId = null
        )
    }

    /** Refund event: record for financial audit; subscription state unchanged. */
    private fun handleStripeRefund(data: JsonNode) {
        val amount = data.path("amount_refunded").asLong(0)
        val metadata = data.path("metadata")
        val parentIdStr = metadata.path("parent_id").asText().takeIf { it.isNotBlank() } ?: return
        val parentId = parentIdStr.toLongOrNull() ?: return
        val invoiceId = data.path("invoice").asText().takeIf { it.isNotBlank() }
        subscriptionService.recordRefund(
            parentId = parentId,
            subscriptionId = null,
            provider = PaymentProvider.STRIPE,
            externalId = data.path("id").asText().takeIf { it.isNotBlank() },
            amountMinor = amount,
            invoiceId = invoiceId
        )
    }

    private fun mapStripeStatus(s: String?): SubscriptionStatus = when (s) {
        "trialing" -> SubscriptionStatus.TRIAL
        "active" -> SubscriptionStatus.ACTIVE
        "past_due" -> SubscriptionStatus.PAST_DUE
        "canceled", "unpaid" -> SubscriptionStatus.EXPIRED
        else -> SubscriptionStatus.ACTIVE
    }

    private fun mapStripePlan(s: String): SubscriptionPlan = try {
        SubscriptionPlan.valueOf(s.uppercase().replace("-", "_"))
    } catch (_: Exception) {
        SubscriptionPlan.PREMIUM_MONTHLY
    }

    private fun handleZohoEvent(eventId: String, eventType: String, node: JsonNode) {
        when (eventType) {
            "payment.succeeded" -> handleZohoPaymentSucceeded(node)
            "payment_link.paid" -> handleZohoPaymentLinkPaid(node)
            else -> log.debug("Unhandled Zoho event: {}", eventType)
        }
    }

    private fun handleZohoPaymentSucceeded(node: JsonNode) {
        val eventObject = node.path("event_object")
        val payment = eventObject.path("payment")
        val paymentId = payment.path("payment_id").asText().takeIf { it.isNotBlank() } ?: return
        val amountStr = payment.path("amount").asText("0").replace(".", "").take(12)
        val amountMinor = amountStr.toLongOrNull() ?: 0
        val currency = payment.path("currency").asText("INR")
        val metaData = payment.path("meta_data")
        var parentIdStr: String? = null
        if (metaData.isArray) {
            for (i in 0 until metaData.size()) {
                val entry = metaData[i]
                if (entry.path("key").asText() == "parent_id") {
                    parentIdStr = entry.path("value").asText().takeIf { it.isNotBlank() }
                    break
                }
            }
        }
        if (parentIdStr == null) return
        val parentId = parentIdStr.toLongOrNull() ?: return
        subscriptionService.recordPaymentSucceeded(
            parentId = parentId,
            subscriptionId = null,
            provider = PaymentProvider.ZOHO,
            externalId = paymentId,
            amountMinor = amountMinor,
            currency = currency,
            invoiceId = payment.path("invoice_number").asText().takeIf { it.isNotBlank() }
        )
    }

    private fun handleZohoPaymentLinkPaid(node: JsonNode) {
        val eventObject = node.path("event_object")
        val paymentLinks = eventObject.path("payment_links")
        val payments = paymentLinks.path("payments")
        if (!payments.isArray || payments.isEmpty()) return
        val firstPayment = payments[0]
        val paymentId = firstPayment.path("payment_id").asText().takeIf { it.isNotBlank() } ?: return
        val amountStr = firstPayment.path("amount").asText("0").replace(".", "").take(12)
        val amountMinor = amountStr.toLongOrNull() ?: 0
        val currency = paymentLinks.path("currency").asText("INR")
        val referenceId = paymentLinks.path("reference_id").asText().takeIf { it.isNotBlank() }
        val parentId = referenceId?.split("_")?.lastOrNull()?.toLongOrNull() ?: return
        subscriptionService.recordPaymentSucceeded(
            parentId = parentId,
            subscriptionId = null,
            provider = PaymentProvider.ZOHO,
            externalId = paymentId,
            amountMinor = amountMinor,
            currency = currency,
            invoiceId = null
        )
    }
}
