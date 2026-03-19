package com.tamixa.api.deeplink

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * Public redirect for share links. GET /s/{storyId} redirects to app scheme so
 * opening https://tamixa.com/s/123 (or configured base) in a browser can open the app.
 * Mobile apps register tamixa://story/{storyId} to handle this.
 */
@RestController
@RequestMapping("/s")
class ShareRedirectController {

    @GetMapping("/{storyId}")
    fun redirectToStory(@PathVariable storyId: Long): ResponseEntity<Void> {
        val scheme = "tamixa://story/$storyId"
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(scheme)).build()
    }
}
