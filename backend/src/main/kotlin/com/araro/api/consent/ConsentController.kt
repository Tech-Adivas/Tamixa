package com.araro.api.consent

import com.araro.api.ApiVersion
import com.araro.api.consent.dto.ConsentRecordResponse
import com.araro.api.consent.dto.RecordConsentRequest
import com.araro.application.consent.ConsentService
import com.araro.application.consent.ConsentRecord
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/consent")
@PreAuthorize("hasRole('PARENT')")
class ConsentController(
    private val consentService: ConsentService
) {
    @GetMapping
    fun list(): ResponseEntity<List<ConsentRecordResponse>> {
        val email = currentParentEmail()
        val records = consentService.listByParent(email)
        return ResponseEntity.ok(records.map { ConsentRecordResponse(it.consentType, it.version, it.grantedAt.toString()) })
    }

    @PostMapping
    fun record(@jakarta.validation.Valid @RequestBody request: RecordConsentRequest): ResponseEntity<Unit> {
        val email = currentParentEmail()
        consentService.record(email, request.consentType, request.version ?: 1)
        return ResponseEntity.ok().build()
    }

    private fun currentParentEmail(): String =
        SecurityContextHolder.getContext().authentication?.name ?: throw IllegalStateException("Not authenticated")
}
