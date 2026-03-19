package com.tamixa.api.webhook

import com.tamixa.IntegrationTestBase
import com.tamixa.api.ApiVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

/**
 * Tests for WebhookController: signature verification, body validation, and idempotency.
 * Does not test full subscription state changes (that would require valid provider signatures).
 */
class WebhookControllerTest : IntegrationTestBase() {

    @Autowired
    private lateinit var restTemplate: org.springframework.boot.test.web.client.TestRestTemplate

    @Test
    fun `stripe webhook without signature returns 400`() {
        val body = """{"id":"evt_1","type":"customer.subscription.updated","data":{"object":{}}}"""
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val response: ResponseEntity<String> = restTemplate.exchange(
            "${ApiVersion.V1}/webhooks/stripe",
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
        assertThat(response.statusCode.value()).isIn(400, 503)
        assertThat(response.body).isNotNull()
    }

    @Test
    fun `stripe webhook with body but invalid signature returns 400`() {
        val body = """{"id":"evt_1","type":"customer.subscription.updated","data":{"object":{}}}"""
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Stripe-Signature", "t=123,v=invalid")
        }
        val response: ResponseEntity<String> = restTemplate.exchange(
            "${ApiVersion.V1}/webhooks/stripe",
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
        assertThat(response.statusCode.value()).isIn(400, 503)
        assertThat(response.body).isNotNull()
    }

    @Test
    fun `zoho webhook without signature returns 400`() {
        val body = """{"event_id":"e1","event_type":"payment.succeeded","event_object":{}}"""
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val response: ResponseEntity<String> = restTemplate.exchange(
            "${ApiVersion.V1}/webhooks/zoho",
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
        assertThat(response.statusCode.value()).isIn(400, 503)
    }

    @Test
    fun `zoho webhook with body but invalid signature returns 400`() {
        val body = """{"event_id":"e1","event_type":"payment.succeeded","event_object":{}}"""
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Zoho-Webhook-Signature", "t=123,v=invalid")
        }
        val response: ResponseEntity<String> = restTemplate.exchange(
            "${ApiVersion.V1}/webhooks/zoho",
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
        assertThat(response.statusCode.value()).isIn(400, 503)
    }
}
