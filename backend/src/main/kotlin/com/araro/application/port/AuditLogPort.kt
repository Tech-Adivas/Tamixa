package com.araro.application.port

/**
 * Port for structured audit logging. Used for security and compliance:
 * login attempts, story generation, voice upload, subscription changes.
 */
interface AuditLogPort {

    fun logLoginAttempt(email: String, success: Boolean, traceId: String?)

    fun logOtpLoginAttempt(maskedPhone: String, success: Boolean, traceId: String?)

    fun logStoryGeneration(storyId: Long, parentId: Long, theme: String, traceId: String?)

    fun logVoiceUpload(parentId: Long, voiceProfileId: Long, traceId: String?)

    fun logVoiceDelete(parentId: Long, voiceId: Long, traceId: String?)

    fun logSubscriptionChange(parentId: Long, action: String, details: String?, traceId: String?)

    fun logAdminAction(adminEmail: String, action: String, resourceType: String, resourceId: String?, details: String?, traceId: String?)

    /** GDPR/data compliance: log when a parent requests data export. */
    fun logDataExportRequest(parentId: Long, jobId: Long, traceId: String?)
}
