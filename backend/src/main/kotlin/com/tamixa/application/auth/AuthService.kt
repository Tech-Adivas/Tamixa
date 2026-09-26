package com.tamixa.application.auth

import com.tamixa.application.consent.ConsentService
import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.DevicePort
import com.tamixa.application.port.EmailSenderPort
import com.tamixa.application.port.JwtPort
import com.tamixa.application.port.MagicLinkTokenRepositoryPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.domain.Parent
import com.tamixa.domain.Role
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)

@Service
class AuthService(
    private val parentRepository: ParentRepositoryPort,
    private val jwt: JwtPort,
    private val passwordEncoder: PasswordEncoder,
    private val auditLog: AuditLogPort,
    private val devicePort: DevicePort,
    private val consentService: ConsentService,
    private val magicLinkTokenRepository: MagicLinkTokenRepositoryPort,
    private val emailSender: EmailSenderPort,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAGIC_LINK_EXPIRY_SECONDS = 15L * 60L // 15 minutes
        private val LOGIN_TOKEN_REGEX = Regex("^[a-f0-9]{32}$")
        private val codeRandom = SecureRandom()
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun randomSixDigitCode(): String {
        val n = codeRandom.nextInt(900_000) + 100_000
        return n.toString()
    }

    /**
     * Register only if email does not exist. If exists, throws (use login instead).
     * Never inserts when email already exists.
     */
    @Transactional
    fun register(
        email: String,
        password: String,
        acceptedTerms: Boolean = false,
        acceptedPrivacy: Boolean = false,
        acceptedParentalAttestation: Boolean = false
    ): AuthTokens {
        if (!acceptedTerms || !acceptedPrivacy) {
            throw ConsentRequiredException("Terms of Service and Privacy Policy consent are required")
        }
        if (!acceptedParentalAttestation) {
            throw ConsentRequiredException("Parental attestation (I am the parent/guardian and at least 18) is required")
        }
        if (parentRepository.existsByEmail(email)) {
            throw EmailAlreadyExistsException(email)
        }
        val hash = passwordEncoder.encode(password)
        val parent = Parent(
            id = 0,
            email = email,
            passwordHash = hash,
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        val saved = parentRepository.save(parent)
        consentService.record(saved.email, "terms_of_service", 1)
        consentService.record(saved.email, "privacy_policy", 1)
        consentService.record(saved.email, "parental_attestation", 1)
        return issueTokens(saved.email, saved.role.name)
    }

    /** Login for existing users by email+password. No insert. */
    @Transactional
    fun login(email: String, password: String, deviceFingerprint: String? = null): AuthTokens {
        val parent = parentRepository.findByEmail(email)
        if (parent == null) {
            auditLog.logLoginAttempt(email, success = false, traceId = null)
            throw InvalidCredentialsException()
        }
        if (parent.suspendedAt != null) {
            auditLog.logLoginAttempt(email, success = false, traceId = null)
            throw AccountSuspendedException()
        }
        if (!passwordEncoder.matches(password, parent.passwordHash)) {
            auditLog.logLoginAttempt(email, success = false, traceId = null)
            throw InvalidCredentialsException()
        }
        auditLog.logLoginAttempt(email, success = true, traceId = null)
        // Fraud prevention: store hashed device fingerprint for trial eligibility (one device per account)
        deviceFingerprint?.takeIf { it.isNotBlank() }?.let { hash ->
            devicePort.registerDevice(parent.id, hash)
        }
        return issueTokens(parent.email, parent.role.name)
    }

    fun refresh(refreshToken: String): AuthTokens {
        val claims = jwt.validateRefreshToken(refreshToken)
            ?: throw InvalidRefreshTokenException()
        val parent = parentRepository.findByEmail(claims.email)
            ?: throw InvalidRefreshTokenException()
        return issueTokens(parent.email, parent.role.name)
    }

    /** Sends a 6-digit code to the email for passwordless login. Always returns success to avoid email enumeration. */
    @Transactional
    fun requestPasswordless(email: String): Boolean {
        val normalizedEmail = normalizeEmail(email)
        val token = UUID.randomUUID().toString().replace("-", "").lowercase()
        val shortCode = randomSixDigitCode()
        val expiresAt = Instant.now().plusSeconds(MAGIC_LINK_EXPIRY_SECONDS) // 15 minutes
        magicLinkTokenRepository.save(normalizedEmail, token, expiresAt, shortCode)
        val webBase = appProperties.auth.webBaseUrl.trim().removeSuffix("/")
        val magicLink = "$webBase/login?token=$token"
        val sent = emailSender.sendMagicLinkOrCode(normalizedEmail, magicLink, shortCode)
        return sent
    }

    /**
     * Verifies passwordless login using either:
     * - [loginToken]: opaque token from the magic-link URL, or
     * - [email] + [code]: 6-digit code from the email.
     * Creates parent if new; requires consent for new users.
     */
    @Transactional
    fun verifyPasswordless(
        email: String?,
        code: String?,
        loginToken: String?,
        acceptedTerms: Boolean = false,
        acceptedPrivacy: Boolean = false,
        acceptedParentalAttestation: Boolean = false
    ): AuthTokens {
        val devCode = appProperties.auth.devPasswordlessCode?.trim()?.takeIf { it.isNotEmpty() }
        val tokenRaw = loginToken?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val emailNorm = email?.let { normalizeEmail(it) }
        val codeRaw = code?.trim()?.takeIf { it.isNotEmpty() }

        val resolvedEmail: String = when {
            !tokenRaw.isNullOrEmpty() -> verifyPasswordlessWithLoginToken(tokenRaw)
            !emailNorm.isNullOrEmpty() && !codeRaw.isNullOrEmpty() ->
                verifyPasswordlessWithShortCode(emailNorm, codeRaw!!, devCode)
            else -> throw InvalidCredentialsException()
        }

        var parent = parentRepository.findByEmail(resolvedEmail)
        if (parent == null) {
            if (!acceptedTerms || !acceptedPrivacy) {
                throw ConsentRequiredException("Terms of Service and Privacy Policy consent are required")
            }
            if (!acceptedParentalAttestation) {
                throw ConsentRequiredException("Parental attestation (I am the parent/guardian and at least 18) is required")
            }
            val hash = passwordEncoder.encode(UUID.randomUUID().toString())
            parent = parentRepository.save(
                Parent(
                    id = 0,
                    email = resolvedEmail,
                    passwordHash = hash,
                    role = Role.PARENT,
                    createdAt = Instant.now()
                )
            )
            consentService.record(parent.email, "terms_of_service", 1)
            consentService.record(parent.email, "privacy_policy", 1)
            consentService.record(parent.email, "parental_attestation", 1)
        }
        if (parent.suspendedAt != null) throw AccountSuspendedException()
        auditLog.logLoginAttempt(resolvedEmail, success = true, traceId = null)
        return issueTokens(parent.email, parent.role.name)
    }

    private fun verifyPasswordlessWithLoginToken(tokenRaw: String): String {
        if (!LOGIN_TOKEN_REGEX.matches(tokenRaw)) {
            throw InvalidCredentialsException()
        }
        val magicToken = magicLinkTokenRepository.findValidByLoginToken(tokenRaw)
            ?: throw InvalidCredentialsException()
        magicLinkTokenRepository.markUsed(magicToken.id)
        return normalizeEmail(magicToken.email)
    }

    private fun verifyPasswordlessWithShortCode(emailNorm: String, codeRaw: String, devCode: String?): String {
        val isDevBypass = devCode != null && codeRaw == devCode
        if (isDevBypass) {
            check(appProperties.auth.devPasswordlessCode?.isNotBlank() == true) {
                "Dev passwordless code must not be used in production"
            }
            log.warn("Passwordless dev bypass used for email verification - ensure DEV_PASSWORDLESS_CODE is unset in production")
            return emailNorm
        }
        val magicToken = magicLinkTokenRepository.findValidByEmailAndCode(emailNorm, codeRaw)
            ?: throw InvalidCredentialsException()
        magicLinkTokenRepository.markUsed(magicToken.id)
        return emailNorm
    }

    fun issueTokens(email: String, role: String): AuthTokens {
        val accessToken = jwt.generateAccessToken(email, role)
        val refreshToken = jwt.generateRefreshToken(email, role)
        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = jwt.getAccessExpirationSeconds()
        )
    }
}

class EmailAlreadyExistsException(email: String) : RuntimeException("Email already registered")

/** Thrown when a phone number is already used by another parent (any role). Ensures one mobile number per account. */
class PhoneAlreadyInUseException(phone: String) : RuntimeException("Phone number already in use by another account")

class InvalidCredentialsException : RuntimeException("Invalid email or password")

class AccountSuspendedException : RuntimeException("Account suspended")

class InvalidRefreshTokenException : RuntimeException("Invalid or expired refresh token")

class ConsentRequiredException(message: String) : RuntimeException(message)
