package com.tamixa.infrastructure.audit

import com.tamixa.application.port.AuditLogPort
import com.tamixa.api.config.RequestTracingFilter
import com.tamixa.infrastructure.logging.PiiMask
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Structured JSON audit logging for production compliance.
 * Logs to SLF4J; in production these can be shipped to a SIEM or log aggregator.
 */
@Component
class StructuredAuditLogger(
    private val objectMapper: ObjectMapper
) : AuditLogPort {

    private val log = LoggerFactory.getLogger("AUDIT")

    override fun logLoginAttempt(email: String, success: Boolean, traceId: String?) {
        val event = mapOf(
            "event" to "login_attempt",
            "email" to PiiMask.maskEmail(email),
            "success" to success,
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logOtpLoginAttempt(maskedPhone: String, success: Boolean, traceId: String?) {
        val event = mapOf(
            "event" to "otp_login_attempt",
            "maskedPhone" to maskedPhone,
            "success" to success,
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logStoryGeneration(storyId: Long, parentId: Long, theme: String, traceId: String?) {
        val event = mapOf(
            "event" to "story_generation",
            "storyId" to storyId,
            "parentId" to parentId,
            "theme" to theme,
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logVoiceUpload(parentId: Long, voiceProfileId: Long, traceId: String?) {
        val event = mapOf(
            "event" to "voice_upload",
            "parentId" to parentId,
            "voiceProfileId" to voiceProfileId,
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logVoiceDelete(parentId: Long, voiceId: Long, traceId: String?) {
        val event = mapOf(
            "event" to "voice_delete",
            "parentId" to parentId,
            "voiceId" to voiceId,
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logSubscriptionChange(parentId: Long, action: String, details: String?, traceId: String?) {
        val event = mapOf(
            "event" to "subscription_change",
            "parentId" to parentId,
            "action" to action,
            "details" to (details ?: ""),
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logAdminAction(adminEmail: String, action: String, resourceType: String, resourceId: String?, details: String?, traceId: String?) {
        val event = mapOf(
            "event" to "admin_action",
            "adminEmail" to PiiMask.maskEmail(adminEmail),
            "action" to action,
            "resourceType" to resourceType,
            "resourceId" to (resourceId ?: ""),
            "details" to (details ?: ""),
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

    override fun logDataExportRequest(parentId: Long, jobId: Long, traceId: String?) {
        val event = mapOf(
            "event" to "data_export_request",
            "parentId" to parentId,
            "jobId" to jobId,
            "traceId" to (traceId ?: MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()),
            "timestamp" to Instant.now().toString()
        )
        log.info(objectMapper.writeValueAsString(event))
    }

}
