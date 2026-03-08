package com.araro.application.auth

import com.araro.application.consent.ConsentService
import com.araro.application.port.AuditLogPort
import com.araro.application.port.DevicePort
import com.araro.application.port.EmailSenderPort
import com.araro.application.port.JwtPort
import com.araro.application.port.MagicLinkTokenRepositoryPort
import com.araro.application.port.ParentRepositoryPort
import com.araro.domain.Parent
import com.araro.domain.Role
import com.araro.infrastructure.config.AppProperties
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
        val token = UUID.randomUUID().toString().replace("-", "")
        val shortCode = (100000..999999).random().toString()
        val expiresAt = Instant.now().plusSeconds(15 * 60) // 15 minutes
        magicLinkTokenRepository.save(email, token, expiresAt, shortCode)
        val webBase = appProperties.auth.webBaseUrl.trim().removeSuffix("/")
        val magicLink = "$webBase/login?token=$token"
        val sent = emailSender.sendMagicLinkOrCode(email, magicLink, shortCode)
        return sent
    }

    /** Verifies the email code and returns tokens. Creates parent if new; requires consent for new users. */
    @Transactional
    fun verifyPasswordless(
        email: String,
        code: String,
        acceptedTerms: Boolean = false,
        acceptedPrivacy: Boolean = false,
        acceptedParentalAttestation: Boolean = false
    ): AuthTokens {
        val devCode = appProperties.auth.devPasswordlessCode
        if (devCode.isNullOrBlank() || code != devCode) {
            val magicToken = magicLinkTokenRepository.findValidByEmailAndCode(email, code)
                ?: throw InvalidCredentialsException()
            magicLinkTokenRepository.markUsed(magicToken.id)
        }
        var parent = parentRepository.findByEmail(email)
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
                    email = email,
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
        auditLog.logLoginAttempt(email, success = true, traceId = null)
        return issueTokens(parent.email, parent.role.name)
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

class InvalidCredentialsException : RuntimeException("Invalid email or password")

class AccountSuspendedException : RuntimeException("Account suspended")

class InvalidRefreshTokenException : RuntimeException("Invalid or expired refresh token")

class ConsentRequiredException(message: String) : RuntimeException(message)
