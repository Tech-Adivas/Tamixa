package com.tamixa.api.home

import com.tamixa.api.ApiVersion
import com.tamixa.application.home.HomeResponse
import com.tamixa.application.home.HomeService
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Home screen aggregator: Continue Adventure, Recommended, Categories, Popular.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/home")
@PreAuthorize("hasRole('PARENT')")
class HomeController(
    private val homeService: HomeService,
    private val parentJpaRepository: ParentJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getHome(
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) childId: Long?
    ): ResponseEntity<HomeResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val parent = parentJpaRepository.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        log.debug("Home data parentId={} language={} childId={}", parent.id, language, childId)
        val response = homeService.getHomeData(
            parentEmail = email,
            parentId = parent.id,
            language = language.trim().lowercase(),
            childId = childId
        )
        return ResponseEntity.ok(response)
    }
}
