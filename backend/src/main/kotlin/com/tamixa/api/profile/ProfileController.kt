package com.tamixa.api.profile

import com.tamixa.api.ApiVersion
import com.tamixa.application.profile.ProfileResponse
import com.tamixa.application.profile.ProfileService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Aggregated user profile: parent, children, subscription.
 * Single call for mobile app boot / profile screen.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/profile")
@PreAuthorize("hasRole('PARENT')")
class ProfileController(
    private val profileService: ProfileService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getProfile(): ResponseEntity<ProfileResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val profile = profileService.getProfile(email)
        return if (profile != null) ResponseEntity.ok(profile)
        else ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }
}
