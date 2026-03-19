package com.tamixa.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Sets storyId and language in MDC for story-related requests so logs show context.
 * Parses path (e.g. /stories/32/...) and query (language=ta) so 404s and errors
 * for story endpoints include masterStoryId and language in the log line.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
class StoryContextMdcFilter : OncePerRequestFilter() {

    companion object {
        const val MDC_MASTER_STORY_ID = "masterStoryId"
        const val MDC_LANGUAGE = "language"

        private val CURATED_ID_IN_PATH = Regex("""/stories/(\d+)(?:/|$)""")
        private val STREAM_LIBRARY_ID = Regex("""/library/(\d+)(?:/|$)""")
        private val STREAM_GENERATED_ID = Regex("""/generated/(\d+)(?:/|$)""")
        private val STREAM_ID = Regex("""/stream/(?:curated|generated)?/(\d+)(?:/|$)""")
        private val STORIES_ID = Regex("""/stories/(\d+)(?:/|$)""")
        private val DEV_TRIGGER_RETRY = Regex("""/dev/trigger-retry/(\d+)(?:/|$)""")
        private val DEV_TRIGGER_PIPELINE = Regex("""/dev/trigger-pipeline/(\d+)(?:/|$)""")
        private val DEV_STORY_STATUS = Regex("""/dev/story-status/(\d+)(?:/|$)""")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val uri = request.requestURI
            val storyId = parseStoryId(uri)
            val lang = request.getParameter("language")?.take(20)?.trim()?.ifBlank { null }

            if (storyId != null) {
                MDC.put(MDC_MASTER_STORY_ID, storyId.toString())
            }
            if (lang != null) {
                MDC.put(MDC_LANGUAGE, lang)
            }
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_MASTER_STORY_ID)
            MDC.remove(MDC_LANGUAGE)
        }
    }

    private fun parseStoryId(uri: String): Long? {
        CURATED_ID_IN_PATH.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        STREAM_LIBRARY_ID.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        STREAM_GENERATED_ID.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        STORIES_ID.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        DEV_TRIGGER_RETRY.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        DEV_TRIGGER_PIPELINE.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        DEV_STORY_STATUS.find(uri)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        return null
    }
}
