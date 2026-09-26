package com.tamixa.api.exception

import com.tamixa.api.config.RequestTracingFilter
import com.tamixa.application.account.AccountDeletionException
import com.tamixa.application.auth.AccountSuspendedException
import com.tamixa.application.auth.ConsentRequiredException
import com.tamixa.application.auth.InvalidCredentialsException
import com.tamixa.application.auth.InvalidOtpException
import com.tamixa.application.guardrail.ExternalGuardrailUnavailableException
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.story.FreeStoryLimitReachedException
import com.tamixa.application.subscription.ReferralCodeDuplicateException
import com.tamixa.application.subscription.ReferralCodeNotFoundException
import com.tamixa.application.subscription.SubscriptionNotFoundException
import com.tamixa.application.storylibrary.ContentUnchangedException
import com.tamixa.application.controlplane.AiControlPlaneConflictException
import com.tamixa.application.controlplane.AiControlPlaneEntityNotFoundException
import com.tamixa.application.storylibrary.PipelineRunningException
import com.tamixa.application.familyvoice.FamilyVoiceAccessDeniedException
import com.tamixa.application.familyvoice.FamilyVoiceFileTooLargeException
import com.tamixa.application.familyvoice.FamilyVoiceNotFoundException
import com.tamixa.application.familyvoice.InvalidVoiceFileException as FamilyInvalidVoiceFileException
import com.tamixa.application.avatar.AvatarFileTooLargeException
import com.tamixa.application.avatar.AvatarPremiumRequiredException
import com.tamixa.application.avatar.InvalidAvatarFileException
import com.tamixa.application.avatar.AvatarNotFoundException
import com.tamixa.api.exception.LimitReachedResponse
import com.tamixa.domain.narration.NarrationJobCapacityExceededException
import com.tamixa.domain.subscription.UpgradeRequiredException
import com.tamixa.infrastructure.narration.NarrationLLMException
import com.tamixa.infrastructure.openai.OpenAIException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun errorBody(message: String, status: HttpStatus, errors: Map<String, String>? = null): Map<String, Any> {
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        return ErrorResponse.body(message, status.value(), traceId.ifBlank { null }, errors)
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException): ResponseEntity<Map<String, Any>> {
        log.debug("Login failed: invalid credentials")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(e.message ?: "Invalid email or password", HttpStatus.UNAUTHORIZED))
    }

    @ExceptionHandler(InvalidOtpException::class)
    fun handleInvalidOtp(e: InvalidOtpException): ResponseEntity<Map<String, Any>> {
        log.debug("OTP verify failed: {}", e.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(errorBody(e.message ?: "Invalid or expired OTP", HttpStatus.UNAUTHORIZED))
    }

    @ExceptionHandler(AccountSuspendedException::class)
    fun handleAccountSuspended(e: AccountSuspendedException): ResponseEntity<Map<String, Any>> {
        log.debug("Login failed: account suspended")
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(e.message ?: "Account suspended", HttpStatus.FORBIDDEN))
    }

    @ExceptionHandler(ConsentRequiredException::class)
    fun handleConsentRequired(e: ConsentRequiredException): ResponseEntity<Map<String, Any>> {
        log.debug("Registration rejected: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorBody(e.message ?: "Consent required", HttpStatus.BAD_REQUEST))
    }

    @ExceptionHandler(AccountDeletionException::class)
    fun handleAccountDeletion(e: AccountDeletionException): ResponseEntity<Map<String, Any>> {
        log.debug("Account deletion failed: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e.message ?: "Account could not be deleted", HttpStatus.BAD_REQUEST))
    }

    /** Content moderation failure (story, short content). */
    @ExceptionHandler(ContentModerationException::class)
    fun handleContentModeration(e: ContentModerationException): ResponseEntity<Map<String, Any>> {
        log.debug("Content moderation rejected: {}", e.message)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(errorBody(e.message ?: "Content failed moderation and cannot be saved", HttpStatus.UNPROCESSABLE_ENTITY))
    }

    /** Separate reusable guardrails HTTP service unavailable (fail-open disabled). */
    @ExceptionHandler(ExternalGuardrailUnavailableException::class)
    fun handleExternalGuardrailUnavailable(e: ExternalGuardrailUnavailableException): ResponseEntity<Map<String, Any>> {
        log.warn("External guardrails service unavailable: {}", e.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(errorBody(e.message ?: "Validation service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE))
    }

    /** Free plan story limit reached; return 402 so client can show upgrade CTA. */
    @ExceptionHandler(FreeStoryLimitReachedException::class)
    fun handleFreeStoryLimitReached(e: FreeStoryLimitReachedException): ResponseEntity<LimitReachedResponse> {
        log.debug("Free story limit reached: limit={} recommendedPlan={}", e.limit, e.recommendedPlan)
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            LimitReachedResponse(
                remaining = e.remaining,
                upgradeRequired = true,
                recommendedPlan = e.recommendedPlan
            )
        )
    }

    /** Premium voice / gated feature (402 for client upgrade CTA). */
    @ExceptionHandler(UpgradeRequiredException::class)
    fun handleUpgradeRequired(e: UpgradeRequiredException): ResponseEntity<Map<String, Any>> {
        log.debug("Upgrade required: {}", e.message)
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(errorBody(e.message ?: "Premium subscription required", HttpStatus.PAYMENT_REQUIRED))
    }

    @ExceptionHandler(ReferralCodeNotFoundException::class)
    fun handleReferralCodeNotFound(e: ReferralCodeNotFoundException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.message ?: "Referral code not found", HttpStatus.NOT_FOUND))
    }

    @ExceptionHandler(ReferralCodeDuplicateException::class)
    fun handleReferralCodeDuplicate(e: ReferralCodeDuplicateException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(e.message ?: "Referral code already exists", HttpStatus.CONFLICT))
    }

    @ExceptionHandler(ContentUnchangedException::class)
    fun handleContentUnchanged(e: ContentUnchangedException): ResponseEntity<Map<String, Any>> {
        log.debug("Submit for review rejected: content unchanged")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e.message ?: "Content is the same, cannot submit for review", HttpStatus.BAD_REQUEST))
    }

    @ExceptionHandler(PipelineRunningException::class)
    fun handlePipelineRunning(e: PipelineRunningException): ResponseEntity<Map<String, Any>> {
        log.debug("Submit for review rejected: pipeline already running")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(e.message ?: "Pipeline is already running for this story. Please wait.", HttpStatus.CONFLICT))
    }

    /** Request body validation failures (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("Validation failed", HttpStatus.BAD_REQUEST, errors))
    }

    @ExceptionHandler(AiControlPlaneEntityNotFoundException::class)
    fun handleAiControlPlaneNotFound(e: AiControlPlaneEntityNotFoundException): ResponseEntity<Map<String, Any>> {
        log.debug("AI control plane: {}", e.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.message ?: "Not found", HttpStatus.NOT_FOUND))
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleUnsupportedOperation(e: UnsupportedOperationException): ResponseEntity<Map<String, Any>> {
        log.debug("Not implemented: {}", e.message)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(errorBody(e.message ?: "Not implemented", HttpStatus.NOT_IMPLEMENTED))
    }

    @ExceptionHandler(AiControlPlaneConflictException::class)
    fun handleAiControlPlaneConflict(e: AiControlPlaneConflictException): ResponseEntity<Map<String, Any>> {
        log.debug("AI control plane conflict: {}", e.message)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(e.message ?: "Conflict", HttpStatus.CONFLICT))
    }

    /** Structured 400 with stable JSON [code] for clients (e.g. story generation validation). */
    @ExceptionHandler(ApiBadRequestException::class)
    fun handleApiBadRequest(e: ApiBadRequestException): ResponseEntity<Map<String, Any>> {
        log.debug("Invalid request: code={} message={}", e.code, e.message)
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.body(
                e.message ?: "Invalid request",
                HttpStatus.BAD_REQUEST.value(),
                traceId.ifBlank { null },
                null,
                e.code,
            )
        )
    }

    /** Business rule violations (duplicate title, etc.). */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        log.debug("Invalid request: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e.message ?: "Invalid request", HttpStatus.BAD_REQUEST))
    }

    /** Method not allowed (e.g. POST where GET expected). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(e: HttpRequestMethodNotSupportedException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorBody(e.message ?: "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED))
    }

    /** OpenAI API errors (rate limit, model error, timeout). */
    @ExceptionHandler(OpenAIException::class)
    fun handleOpenAI(e: OpenAIException): ResponseEntity<Map<String, Any>> {
        log.warn("OpenAI API error: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(errorBody(e.message ?: "AI service temporarily unavailable", HttpStatus.BAD_GATEWAY))
    }

    /** Narration / custom-prompt LLM errors (Gemini/OpenAI via [com.tamixa.application.port.narration.NarrationLLMPort]). */
    @ExceptionHandler(NarrationLLMException::class)
    fun handleNarrationLlm(e: NarrationLLMException): ResponseEntity<Map<String, Any>> {
        val msg = e.message?.replace("\n", " ")?.take(2000) ?: "LLM request failed"
        log.warn("Narration LLM error: {}", msg)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(errorBody(msg, HttpStatus.BAD_GATEWAY))
    }

    /**
     * Client disconnected while response was streaming/writing (e.g. navigation, emulator rotation, Ktor cancel).
     * Not a server failure; DEBUG only (see application.yml note on Broken pipe noise).
     */
    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handleClientAbort(e: AsyncRequestNotUsableException): ResponseEntity<Void> {
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        log.debug("Client connection closed before response completed traceId={} message={}", traceId, e.message)
        return ResponseEntity.noContent().build()
    }

    /** Catch-all for unhandled exceptions; logs with traceId for correlation. */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, Any>> {
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        log.error("Unhandled exception traceId={} exception={}", traceId, e.message, e)
        val message = e.message?.take(200) ?: "An unexpected error occurred"
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(message, HttpStatus.INTERNAL_SERVER_ERROR))
    }
}
