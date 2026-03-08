package com.araro.api.favorite

import com.araro.api.ApiVersion
import com.araro.application.favorite.FavoriteStoryService
import com.araro.domain.FavoriteStory
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/favorites")
@PreAuthorize("hasRole('PARENT')")
class FavoriteStoryController(
    private val favoriteStoryService: FavoriteStoryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun list(): ResponseEntity<List<FavoriteStoryResponse>> {
        log.debug("Favorite list requested")
        val email = currentParentEmail()
        val favorites = favoriteStoryService.listByParent(email)
        return ResponseEntity.ok(favorites.map { FavoriteStoryResponse(it.storyId, it.storySource) })
    }

    @PostMapping("/{storyId}")
    fun add(
        @PathVariable storyId: Long,
        @RequestParam(defaultValue = "generated") storySource: String
    ): ResponseEntity<FavoriteStoryResponse> {
        val email = currentParentEmail()
        log.info("Favorite add storyId={} source={}", storyId, storySource)
        val fav = favoriteStoryService.add(email, storyId, storySource)
        return ResponseEntity.ok(FavoriteStoryResponse(fav.storyId, fav.storySource))
    }

    @DeleteMapping("/{storyId}")
    fun remove(@PathVariable storyId: Long): ResponseEntity<Unit> {
        val email = currentParentEmail()
        log.info("Favorite remove storyId={}", storyId)
        favoriteStoryService.remove(email, storyId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{storyId}/check")
    fun check(@PathVariable storyId: Long): ResponseEntity<FavoriteCheckResponse> {
        val email = currentParentEmail()
        val isFav = favoriteStoryService.isFavorite(email, storyId)
        return ResponseEntity.ok(FavoriteCheckResponse(storyId, isFav))
    }

    private fun currentParentEmail(): String =
        SecurityContextHolder.getContext().authentication?.name ?: throw IllegalStateException("Not authenticated")
}

data class FavoriteStoryResponse(val storyId: Long, val storySource: String)
data class FavoriteCheckResponse(val storyId: Long, val isFavorite: Boolean)
