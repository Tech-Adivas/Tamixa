package com.tamixa.api.auth

import com.tamixa.application.account.AccountDeletionService
import com.tamixa.api.auth.dto.AuthResponse
import com.tamixa.api.auth.dto.CurrentUserResponse
import com.tamixa.api.auth.dto.OtpSendRequest
import com.tamixa.api.auth.dto.OtpSendResponse
import com.tamixa.api.auth.dto.OtpVerifyRequest
import com.tamixa.api.auth.dto.PasswordlessRequest
import com.tamixa.api.auth.dto.PasswordlessSendResponse
import com.tamixa.api.auth.dto.PasswordlessVerifyRequest
import com.tamixa.infrastructure.logging.PiiMask
import com.tamixa.api.auth.dto.LoginRequest
import com.tamixa.api.auth.dto.UpdateProfileRequest
import com.tamixa.api.auth.dto.RefreshTokenRequest
import com.tamixa.api.auth.dto.RegisterRequest
import com.tamixa.application.auth.AuthService
import com.tamixa.application.auth.OtpService
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.profile.ProfileService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.tamixa.api.ApiVersion
import com.tamixa.domain.Role
import com.tamixa.domain.RolePermissions

@RestController
@RequestMapping("${ApiVersion.V1}/auth")
class AuthController(
    private val authService: AuthService,
    private val otpService: OtpService,
    private val accountDeletionService: AccountDeletionService,
    private val parentRepository: ParentRepositoryPort,
    private val profileService: ProfileService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        log.info("Auth register request email={}", PiiMask.maskEmail(request.email))
        val tokens = authService.register(
            request.email,
            request.password,
            request.acceptedTerms,
            request.acceptedPrivacy,
            request.acceptedParentalAttestation
        )
        log.info("Auth register success email={}", PiiMask.maskEmail(request.email))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                AuthResponse(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresInSeconds = tokens.expiresInSeconds
                )
            )
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        log.info("Auth login request email={}", PiiMask.maskEmail(request.email))
        val tokens = authService.login(request.email, request.password, request.deviceFingerprint)
        log.info("Auth login success email={}", PiiMask.maskEmail(request.email))
        return ResponseEntity.ok(
            AuthResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresInSeconds = tokens.expiresInSeconds
            )
        )
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        log.debug("Auth refresh token request")
        val tokens = authService.refresh(request.refreshToken)
        log.info("Auth refresh success")
        return ResponseEntity.ok(
            AuthResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresInSeconds = tokens.expiresInSeconds
            )
        )
    }

    @PostMapping("/otp/send")
    fun sendOtp(@Valid @RequestBody request: OtpSendRequest): ResponseEntity<OtpSendResponse> {
        log.info("OTP send request phone={}", maskPhone(request.phone))
        val result = otpService.sendOtp(request.phone)
        log.info("OTP send completed phone={} sent={}", maskPhone(request.phone), result.sent)
        return ResponseEntity.ok(OtpSendResponse(sent = result.sent, code = result.devCode))
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(@Valid @RequestBody request: OtpVerifyRequest): ResponseEntity<AuthResponse> {
        log.info("OTP verify request phone={}", maskPhone(request.phone))
        val tokens = otpService.verifyOtp(request.phone, request.code)
        log.info("OTP verify success phone={}", maskPhone(request.phone))
        return ResponseEntity.ok(
            AuthResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresInSeconds = tokens.expiresInSeconds
            )
        )
    }

    @PostMapping("/passwordless")
    fun requestPasswordless(@Valid @RequestBody request: PasswordlessRequest): ResponseEntity<PasswordlessSendResponse> {
        log.info("Passwordless request email={}", PiiMask.maskEmail(request.email))
        val sent = authService.requestPasswordless(request.email)
        log.info("Passwordless send completed email={} sent={}", PiiMask.maskEmail(request.email), sent)
        return ResponseEntity.ok(PasswordlessSendResponse(sent = sent))
    }

    @PostMapping("/passwordless/verify")
    fun verifyPasswordless(@Valid @RequestBody request: PasswordlessVerifyRequest): ResponseEntity<AuthResponse> {
        log.info("Passwordless verify request email={}", PiiMask.maskEmail(request.email))
        val tokens = authService.verifyPasswordless(
            request.email,
            request.code,
            request.acceptedTerms,
            request.acceptedPrivacy,
            request.acceptedParentalAttestation
        )
        log.info("Passwordless verify success email={}", PiiMask.maskEmail(request.email))
        return ResponseEntity.ok(
            AuthResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresInSeconds = tokens.expiresInSeconds
            )
        )
    }

    private fun maskPhone(phone: String) = PiiMask.maskPhone(phone)

    @GetMapping("/me")
    fun me(): ResponseEntity<CurrentUserResponse> {
        val auth = SecurityContextHolder.getContext().authentication ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val email = auth.name
        val roleStr = auth.authorities.firstOrNull()?.authority?.removePrefix("ROLE_") ?: "PARENT"
        val roleEnum = runCatching { Role.valueOf(roleStr) }.getOrNull() ?: Role.PARENT
        val permissions = if (roleEnum == Role.PARENT) emptyList()
        else RolePermissions.permissionsFor(roleEnum).map { it.name }
        val parent = parentRepository.findByEmail(email)
        val nickname = parent?.nickname
        val displayName = parent?.displayName
        return ResponseEntity.ok(
            CurrentUserResponse(
                email = email,
                role = roleStr,
                nickname = nickname,
                displayName = displayName,
                permissions = permissions
            )
        )
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('PARENT')")
    fun updateProfile(@Valid @RequestBody request: UpdateProfileRequest): ResponseEntity<Map<String, String>> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val updated = profileService.updateProfile(email, request.nickname, request.displayName)
        return if (updated) ResponseEntity.ok(mapOf("message" to "Profile updated"))
        else ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    /** Permanently delete the authenticated parent account and all associated data (GDPR). */
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('PARENT')")
    fun deleteAccount(): ResponseEntity<Map<String, String>> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        accountDeletionService.deleteAccount(email)
        return ResponseEntity.ok(mapOf("message" to "Account deleted successfully"))
    }
}
