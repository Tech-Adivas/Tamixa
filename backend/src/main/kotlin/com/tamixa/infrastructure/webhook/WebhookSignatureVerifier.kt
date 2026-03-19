package com.tamixa.infrastructure.webhook

import com.tamixa.domain.PaymentProvider
import com.stripe.exception.SignatureVerificationException
import com.stripe.net.Webhook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies webhook signatures. Never trust client-provided subscription state;
 * all subscription changes must come from verified webhooks or server-side logic.
 */
@Component
class WebhookSignatureVerifier {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @param rawBody exact UTF-8 body as received (must not be modified/pretty-printed)
     * @param signatureHeader Stripe-Signature or X-Zoho-Webhook-Signature
     * @param provider STRIPE or ZOHO
     * @param webhookSecret endpoint secret for the provider
     * @return true if verification succeeded
     */
    fun verify(
        rawBody: String,
        signatureHeader: String?,
        provider: PaymentProvider,
        webhookSecret: String
    ): Boolean {
        if (signatureHeader.isNullOrBlank() || webhookSecret.isBlank()) {
            log.warn("Webhook verification skipped: missing signature or secret for {}", provider)
            return false
        }
        return when (provider) {
            PaymentProvider.STRIPE -> verifyStripe(rawBody, signatureHeader, webhookSecret)
            PaymentProvider.ZOHO -> verifyZoho(rawBody, signatureHeader, webhookSecret)
        }
    }

    private fun verifyStripe(payload: String, sigHeader: String, secret: String): Boolean {
        return try {
            Webhook.constructEvent(payload, sigHeader, secret)
            true
        } catch (e: SignatureVerificationException) {
            log.warn("Stripe webhook signature verification failed: {}", e.message)
            false
        }
    }

    /**
     * Zoho uses HMAC-SHA256. Header format: t=timestamp,v=signature
     * Data to sign: {timestamp}.{payload}
     */
    private fun verifyZoho(body: String, signatureHeader: String, signingKey: String): Boolean {
        return try {
            val parts = signatureHeader.split(",").associate { part ->
                val (k, v) = part.split("=", limit = 2)
                k.trim() to v.trim()
            }
            val timestamp = parts["t"] ?: return false
            val signature = parts["v"] ?: return false
            val dataToSign = "$timestamp.$body"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(signingKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val computed = mac.doFinal(dataToSign.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val ok = computed.equals(signature, ignoreCase = true)
            if (!ok) log.warn("Zoho webhook signature mismatch")
            ok
        } catch (e: Exception) {
            log.warn("Zoho webhook signature verification failed: {}", e.message)
            false
        }
    }
}
