package com.tamixa.application.auth

import com.tamixa.application.consent.ConsentService
import com.tamixa.application.port.AuditLogPort
import com.tamixa.application.port.DevicePort
import com.tamixa.application.port.EmailSenderPort
import com.tamixa.application.port.JwtPort
import com.tamixa.application.port.MagicLinkTokenRepositoryPort
import com.tamixa.application.port.TokenClaims
import com.tamixa.application.port.MagicLinkToken
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.domain.Parent
import com.tamixa.domain.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.Mockito.lenient
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var parentRepository: ParentRepositoryPort
    @Mock lateinit var jwt: JwtPort
    @Mock lateinit var passwordEncoder: PasswordEncoder
    @Mock lateinit var auditLog: AuditLogPort
    @Mock lateinit var devicePort: DevicePort
    @Mock lateinit var consentService: ConsentService
    @Mock lateinit var magicLinkTokenRepository: MagicLinkTokenRepositoryPort
    @Mock lateinit var emailSender: EmailSenderPort
    @Mock lateinit var appProperties: AppProperties

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        lenient().`when`(appProperties.auth).thenReturn(AppProperties.AuthProperties())
        authService = AuthService(
            parentRepository = parentRepository,
            jwt = jwt,
            passwordEncoder = passwordEncoder,
            auditLog = auditLog,
            devicePort = devicePort,
            consentService = consentService,
            magicLinkTokenRepository = magicLinkTokenRepository,
            emailSender = emailSender,
            appProperties = appProperties
        )
    }

    @Test
    fun `register without consent throws ConsentRequiredException`() {
        assertThrows(ConsentRequiredException::class.java) {
            authService.register("test@example.com", "password", acceptedTerms = false, acceptedPrivacy = true, acceptedParentalAttestation = true)
        }
        assertThrows(ConsentRequiredException::class.java) {
            authService.register("test@example.com", "password", acceptedTerms = true, acceptedPrivacy = false, acceptedParentalAttestation = true)
        }
        verify(parentRepository, never()).save(any())
    }

    @Test
    fun `register when email exists throws EmailAlreadyExistsException`() {
        `when`(parentRepository.existsByEmail("existing@example.com")).thenReturn(true)

        assertThrows(EmailAlreadyExistsException::class.java) {
            authService.register("existing@example.com", "password", acceptedTerms = true, acceptedPrivacy = true, acceptedParentalAttestation = true)
        }
        verify(parentRepository, never()).save(any())
    }

    @Test
    fun `register with valid input returns tokens`() {
        `when`(parentRepository.existsByEmail("new@example.com")).thenReturn(false)
        `when`(passwordEncoder.encode("password")).thenReturn("hashed")
        val savedParent = Parent(
            id = 1L,
            email = "new@example.com",
            passwordHash = "hashed",
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        `when`(parentRepository.save(any())).thenReturn(savedParent)
        `when`(jwt.generateAccessToken(any(), any())).thenReturn("access")
        `when`(jwt.generateRefreshToken(any(), any())).thenReturn("refresh")
        `when`(jwt.getAccessExpirationSeconds()).thenReturn(900L)

        val result = authService.register("new@example.com", "password", acceptedTerms = true, acceptedPrivacy = true, acceptedParentalAttestation = true)

        assertEquals("access", result.accessToken)
        assertEquals("refresh", result.refreshToken)
        assertEquals(900L, result.expiresInSeconds)
        verify(consentService).record("new@example.com", "terms_of_service", 1)
        verify(consentService).record("new@example.com", "privacy_policy", 1)
        verify(consentService).record("new@example.com", "parental_attestation", 1)
    }

    @Test
    fun `register without parental attestation throws ConsentRequiredException`() {
        assertThrows(ConsentRequiredException::class.java) {
            authService.register("test@example.com", "password", acceptedTerms = true, acceptedPrivacy = true, acceptedParentalAttestation = false)
        }
        verify(parentRepository, never()).save(any())
    }

    @Test
    fun `login with invalid password throws InvalidCredentialsException`() {
        val parent = Parent(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        `when`(parentRepository.findByEmail("user@example.com")).thenReturn(parent)
        `when`(passwordEncoder.matches("wrong", "hashed")).thenReturn(false)

        assertThrows(InvalidCredentialsException::class.java) {
            authService.login("user@example.com", "wrong")
        }
        verify(auditLog).logLoginAttempt("user@example.com", success = false, traceId = null)
    }

    @Test
    fun `login with valid credentials returns tokens`() {
        val parent = Parent(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        `when`(parentRepository.findByEmail("user@example.com")).thenReturn(parent)
        `when`(passwordEncoder.matches("password", "hashed")).thenReturn(true)
        `when`(jwt.generateAccessToken(any(), any())).thenReturn("access")
        `when`(jwt.generateRefreshToken(any(), any())).thenReturn("refresh")
        `when`(jwt.getAccessExpirationSeconds()).thenReturn(900L)

        val result = authService.login("user@example.com", "password")

        assertEquals("access", result.accessToken)
        assertEquals("refresh", result.refreshToken)
        verify(auditLog).logLoginAttempt("user@example.com", success = true, traceId = null)
    }

    @Test
    fun `refresh with valid token returns new tokens`() {
        `when`(jwt.validateRefreshToken("valid-token")).thenReturn(
            TokenClaims(email = "user@example.com", role = "PARENT")
        )
        val parent = Parent(
            id = 1L,
            email = "user@example.com",
            passwordHash = "hashed",
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        `when`(parentRepository.findByEmail("user@example.com")).thenReturn(parent)
        `when`(jwt.generateAccessToken(any(), any())).thenReturn("new-access")
        `when`(jwt.generateRefreshToken(any(), any())).thenReturn("new-refresh")
        `when`(jwt.getAccessExpirationSeconds()).thenReturn(900L)

        val result = authService.refresh("valid-token")

        assertEquals("new-access", result.accessToken)
        assertEquals("new-refresh", result.refreshToken)
    }

    @Test
    fun `refresh with invalid token throws InvalidRefreshTokenException`() {
        `when`(jwt.validateRefreshToken("invalid-token")).thenReturn(null)

        assertThrows(InvalidRefreshTokenException::class.java) {
            authService.refresh("invalid-token")
        }
    }

    @Test
    fun `verifyPasswordless with email and short code marks token used and returns tokens`() {
        val magic = MagicLinkToken(
            id = 1L,
            email = "u@example.com",
            token = "abc",
            expiresAt = Instant.now().plusSeconds(600),
            shortCode = "123456"
        )
        `when`(magicLinkTokenRepository.findValidByEmailAndCode("u@example.com", "123456")).thenReturn(magic)
        val parent = Parent(
            id = 1L,
            email = "u@example.com",
            passwordHash = "h",
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        `when`(parentRepository.findByEmail("u@example.com")).thenReturn(parent)
        `when`(jwt.generateAccessToken(any(), any())).thenReturn("access")
        `when`(jwt.generateRefreshToken(any(), any())).thenReturn("refresh")
        `when`(jwt.getAccessExpirationSeconds()).thenReturn(900L)

        val result = authService.verifyPasswordless(
            email = "u@example.com",
            code = "123456",
            loginToken = null,
            acceptedTerms = false,
            acceptedPrivacy = false,
            acceptedParentalAttestation = false
        )

        assertEquals("access", result.accessToken)
        verify(magicLinkTokenRepository).markUsed(1L)
        verify(auditLog).logLoginAttempt("u@example.com", success = true, traceId = null)
    }

    @Test
    fun `verifyPasswordless with loginToken marks token used and returns tokens`() {
        val tokenHex = "a".repeat(32)
        val magic = MagicLinkToken(
            id = 2L,
            email = "Link@Example.com",
            token = tokenHex,
            expiresAt = Instant.now().plusSeconds(600),
            shortCode = "999999"
        )
        `when`(magicLinkTokenRepository.findValidByLoginToken(tokenHex)).thenReturn(magic)
        val parent = Parent(
            id = 1L,
            email = "link@example.com",
            passwordHash = "h",
            role = Role.PARENT,
            createdAt = Instant.now()
        )
        `when`(parentRepository.findByEmail("link@example.com")).thenReturn(parent)
        `when`(jwt.generateAccessToken(any(), any())).thenReturn("access")
        `when`(jwt.generateRefreshToken(any(), any())).thenReturn("refresh")
        `when`(jwt.getAccessExpirationSeconds()).thenReturn(900L)

        val result = authService.verifyPasswordless(
            email = null,
            code = null,
            loginToken = tokenHex,
            acceptedTerms = false,
            acceptedPrivacy = false,
            acceptedParentalAttestation = false
        )

        assertEquals("access", result.accessToken)
        verify(magicLinkTokenRepository).markUsed(2L)
        verify(auditLog).logLoginAttempt("link@example.com", success = true, traceId = null)
    }

    @Test
    fun `verifyPasswordless normalizes email for short code lookup`() {
        val magic = MagicLinkToken(
            id = 1L,
            email = "mix@example.com",
            token = "x",
            expiresAt = Instant.now().plusSeconds(600),
            shortCode = "111111"
        )
        `when`(magicLinkTokenRepository.findValidByEmailAndCode("mix@example.com", "111111")).thenReturn(magic)
        `when`(parentRepository.findByEmail("mix@example.com")).thenReturn(
            Parent(1L, "mix@example.com", "h", Role.PARENT, Instant.now())
        )
        `when`(jwt.generateAccessToken(any(), any())).thenReturn("a")
        `when`(jwt.generateRefreshToken(any(), any())).thenReturn("r")
        `when`(jwt.getAccessExpirationSeconds()).thenReturn(900L)

        authService.verifyPasswordless(
            email = "  Mix@Example.Com ",
            code = "111111",
            loginToken = null,
            acceptedTerms = false,
            acceptedPrivacy = false,
            acceptedParentalAttestation = false
        )

        verify(magicLinkTokenRepository).findValidByEmailAndCode("mix@example.com", "111111")
    }
}
