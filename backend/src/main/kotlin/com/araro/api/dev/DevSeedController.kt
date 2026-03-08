package com.araro.api.dev

import com.araro.api.ApiVersion
import com.araro.application.consent.ConsentService
import com.araro.application.port.ParentRepositoryPort
import com.araro.domain.Parent
import com.araro.domain.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Dev-only endpoint to seed/reset admin user. Only active when SEED_ADMIN_ENABLED=true.
 * POST /api/v1/dev/seed-admin creates or updates admin@techadivas.com with password Admin123!
 */
@RestController
@RequestMapping("${ApiVersion.V1}/dev")
@Profile("dev")
class DevSeedController(
    private val parentRepository: ParentRepositoryPort,
    private val passwordEncoder: PasswordEncoder,
    private val consentService: ConsentService,
    @Value("\${SEED_ADMIN_ENABLED:false}") private val seedAdminEnabled: String
) {

    companion object {
        const val ADMIN_EMAIL = "admin@techadivas.com"
        const val ADMIN_PASSWORD = "Admin123!"
    }

    @PostMapping("/seed-admin")
    fun seedAdmin(): ResponseEntity<Map<String, Any>> {
        if (seedAdminEnabled != "true") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("message" to "Seed admin is disabled. Set SEED_ADMIN_ENABLED=true to enable."))
        }
        val existing = parentRepository.findByEmail(ADMIN_EMAIL)
        val hash = passwordEncoder.encode(ADMIN_PASSWORD)
        val parent = if (existing != null) {
            parentRepository.save(
                Parent(
                    id = existing.id,
                    email = ADMIN_EMAIL,
                    passwordHash = hash,
                    role = Role.SUPER_ADMIN,
                    createdAt = existing.createdAt,
                    phone = existing.phone,
                    suspendedAt = null
                )
            )
        } else {
            val created = parentRepository.save(
                Parent(
                    id = 0,
                    email = ADMIN_EMAIL,
                    passwordHash = hash,
                    role = Role.SUPER_ADMIN,
                    createdAt = Instant.now(),
                    phone = null,
                    suspendedAt = null
                )
            )
            consentService.record(created.email, "terms_of_service", 1)
            consentService.record(created.email, "privacy_policy", 1)
            created
        }
        return ResponseEntity.ok(
            mapOf(
                "message" to "Admin seeded successfully",
                "email" to parent.email,
                "role" to parent.role.name
            )
        )
    }
}
