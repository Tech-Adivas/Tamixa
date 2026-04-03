package com.tamixa.application.auth

import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.JwtPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.SmsSenderPort
import com.tamixa.domain.Parent
import com.tamixa.domain.Role
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.logging.PiiMask
import com.tamixa.infrastructure.persistence.OtpVerificationEntity
import com.tamixa.infrastructure.persistence.OtpVerificationJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

/**
 * OTP-based phone login. In dev, app.auth.dev-otp-code (e.g. "123456") bypasses SMS
 * and is accepted with any phone.
 */
@Service
class OtpService(
    private val parentRepository: ParentRepositoryPort,
    private val otpRepository: OtpVerificationJpaRepository,
    private val smsSender: SmsSenderPort,
    private val jwt: JwtPort,
    private val passwordEncoder: PasswordEncoder,
    private val appProperties: AppProperties,
    private val auditLog: AuditLogPort
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val devCode = appProperties.auth.devOtpCode?.takeIf { it.isNotBlank() }

    private val otpExpiryMinutes = 10L

    /** Send OTP to phone. When dev code is set, returns it for UI auto-fill (no real SMS). */
    @Transactional
    fun sendOtp(phone: String): OtpSendResult {
        val normalized = OtpPhoneFormats.normalize(phone) ?: return OtpSendResult(sent = false, devCode = null)
        val code = devCode ?: generateCode()
        val expiresAt = Instant.now().plusSeconds(otpExpiryMinutes * 60)
        if (devCode == null) {
            val entity = OtpVerificationEntity(phone = normalized, code = code, expiresAt = expiresAt)
            val saved = otpRepository.save(entity)
            val delivered = smsSender.sendOtp(normalized, code)
            if (!delivered) {
                otpRepository.delete(saved)
                log.warn("OTP send failed; rolled back stored code for phone={}", PiiMask.maskPhone(normalized))
                return OtpSendResult(sent = false, devCode = null)
            }
        } else {
            log.info("OTP (dev bypass): phone={} (no SMS sent)", PiiMask.maskPhone(normalized))
        }
        return OtpSendResult(sent = true, devCode = devCode)
    }

    /** Verify OTP and return tokens. Accepts dev code when configured. */
    @Transactional
    fun verifyOtp(phone: String, code: String): AuthTokens {
        val normalized = OtpPhoneFormats.normalize(phone) ?: throw InvalidOtpException("Invalid phone number")
        val trimmedCode = code.trim()
        val maskedPhone = PiiMask.maskPhone(normalized)
        if (trimmedCode.isBlank()) {
            auditLog.logOtpLoginAttempt(maskedPhone, success = false, traceId = null)
            throw InvalidOtpException("OTP code is required")
        }
        return try {
            if (devCode != null && trimmedCode == devCode) {
                val tokens = loginOrCreateParent(normalized)
                auditLog.logOtpLoginAttempt(maskedPhone, success = true, traceId = null)
                return tokens
            }
            val entity = otpRepository.findFirstByPhoneAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(normalized, trimmedCode)
            if (entity == null) {
                auditLog.logOtpLoginAttempt(maskedPhone, success = false, traceId = null)
                throw InvalidOtpException("Invalid or expired OTP")
            }
            if (entity.expiresAt.isBefore(Instant.now())) {
                auditLog.logOtpLoginAttempt(maskedPhone, success = false, traceId = null)
                throw InvalidOtpException("OTP has expired")
            }
            entity.usedAt = Instant.now()
            otpRepository.save(entity)
            val tokens = loginOrCreateParent(normalized)
            auditLog.logOtpLoginAttempt(maskedPhone, success = true, traceId = null)
            tokens
        } catch (e: AccountSuspendedException) {
            auditLog.logOtpLoginAttempt(maskedPhone, success = false, traceId = null)
            throw e
        }
    }

    /**
     * Login if parent exists (by phone or synthetic email), else insert new.
     * Never inserts when mobile/email already exists; always allows login for existing users.
     */
    private fun loginOrCreateParent(phone: String): AuthTokens {
        val parent = findExistingParentByPhone(phone)
        if (parent != null) {
            if (parent.suspendedAt != null) throw AccountSuspendedException()
            return issueTokens(parent.email, parent.role.name)
        }
        val syntheticEmail = "$phone@otp.tamixa.in"
        if (parentRepository.existsByEmail(syntheticEmail)) {
            val existing = parentRepository.findByEmail(syntheticEmail)!!
            if (existing.suspendedAt != null) throw AccountSuspendedException()
            return issueTokens(existing.email, existing.role.name)
        }
        val placeholderHash = passwordEncoder.encode(ThreadLocalRandom.current().nextLong().toString())
        val created = parentRepository.save(
            Parent(
                id = 0,
                email = syntheticEmail,
                passwordHash = placeholderHash,
                role = Role.PARENT,
                createdAt = Instant.now(),
                phone = phone,
                suspendedAt = null
            )
        )
        log.info("Created OTP-only parent: phone={}", PiiMask.maskPhone(phone))
        return issueTokens(created.email, created.role.name)
    }

    /**
     * Find existing parent by phone, trying common formats to avoid duplicates.
     * E.g. +919876543210 and 9876543210 refer to the same Indian number.
     */
    private fun findExistingParentByPhone(phone: String): Parent? {
        val formats = OtpPhoneFormats.variantsForStoredPhoneLookup(phone)
        for (fmt in formats) {
            parentRepository.findByPhone(fmt)?.let { return it }
        }
        return null
    }

    private fun generateCode(): String = ThreadLocalRandom.current().nextInt(100_000, 1_000_000).toString()

    private fun issueTokens(email: String, role: String): AuthTokens = AuthTokens(
        accessToken = jwt.generateAccessToken(email, role),
        refreshToken = jwt.generateRefreshToken(email, role),
        expiresInSeconds = jwt.getAccessExpirationSeconds()
    )
}

data class OtpSendResult(val sent: Boolean, val devCode: String? = null)
class InvalidOtpException(message: String) : RuntimeException(message)
