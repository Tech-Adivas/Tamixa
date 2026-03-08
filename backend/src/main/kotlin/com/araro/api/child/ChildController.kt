package com.araro.api.child

import com.araro.api.child.dto.ChildResponse
import com.araro.api.child.dto.CreateChildRequest
import com.araro.api.child.dto.UpdateChildRequest
import com.araro.application.child.ChildService
import com.araro.domain.Child
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.araro.api.ApiVersion

@RestController
@RequestMapping("${ApiVersion.V1}/children")
@PreAuthorize("hasRole('PARENT')")
class ChildController(
    private val childService: ChildService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun create(@Valid @RequestBody request: CreateChildRequest): ResponseEntity<ChildResponse> {
        val parentEmail = currentParentEmail()
        log.debug("Child create requested")
        val child = childService.create(
            parentEmail = parentEmail,
            name = request.name,
            dateOfBirth = request.dateOfBirth,
            languagePreference = request.languagePreference,
            interests = request.interests,
            favoriteColor = request.favoriteColor,
            favoriteAnimal = request.favoriteAnimal,
            characterTraits = request.characterTraits,
            avatarChoice = request.avatarChoice,
            childProfileConsent = request.childProfileConsent
        )
        log.info("Child created childId={}", child.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(child.toResponse())
    }

    @GetMapping
    fun list(): ResponseEntity<List<ChildResponse>> {
        log.debug("Child list requested")
        val parentEmail = currentParentEmail()
        val children = childService.findAllByParent(parentEmail)
        return ResponseEntity.ok(children.map(Child::toResponse))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ChildResponse> {
        val parentEmail = currentParentEmail()
        log.debug("Child getById childId={}", id)
        val child = childService.findByIdForParent(id, parentEmail)
        return ResponseEntity.ok(child.toResponse())
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateChildRequest
    ): ResponseEntity<ChildResponse> {
        val parentEmail = currentParentEmail()
        log.info("Child update requested childId={}", id)
        val child = childService.update(
            parentEmail = parentEmail,
            childId = id,
            languagePreference = request.languagePreference,
            interests = request.interests,
            favoriteColor = request.favoriteColor,
            favoriteAnimal = request.favoriteAnimal,
            characterTraits = request.characterTraits,
            avatarChoice = request.avatarChoice
        )
        return ResponseEntity.ok(child.toResponse())
    }

    private fun currentParentEmail(): String {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        return auth.name
    }
}
