package com.tamixa.api.dev

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.api.admin.dto.LibraryStoryResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Dev-only: parent library clients cannot fetch Tamixa CDN placeholder URLs on a laptop.
 * Rewrites segment [audioUrl] in [LibraryStoryResponse.interactiveGraph] JSON to the classpath-backed
 * placeholder served by [DigitalSurvivalDevController].
 */
fun interface DevDigitalSurvivalParentLibraryPostProcessor {
    fun apply(response: LibraryStoryResponse): LibraryStoryResponse
}

@Component
@Profile("dev")
@ConditionalOnProperty(
    prefix = "app.digital-survival-dev",
    name = ["rewrite-interactive-graph-audio-to-dev-placeholder"],
    havingValue = "true",
)
class DevDigitalSurvivalParentLibraryPostProcessorImpl(
    private val objectMapper: ObjectMapper,
) : DevDigitalSurvivalParentLibraryPostProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("Digital Survival dev: parent library API will rewrite interactive graph CDN segment audio URLs")
    }

    private val audioUrlCdnRegex =
        Regex(
            "\"audioUrl\"\\s*:\\s*\"https://cdn\\.tamixa\\.app/library/sim/digital-survival-guide/[^\"]*\"",
        )

    override fun apply(response: LibraryStoryResponse): LibraryStoryResponse {
        if (!DIGITAL_SURVIVAL_STORY_OWNER.equals(response.storyOwner, ignoreCase = false)) {
            return response
        }
        val graph = response.interactiveGraph ?: return response
        val raw = runCatching { objectMapper.writeValueAsString(graph) }.getOrElse { return response }
        if (!raw.contains(CDN_DIGITAL_SURVIVAL_PREFIX)) return response
        val rewritten =
            raw.replace(audioUrlCdnRegex) {
                "\"audioUrl\":\"$PLACEHOLDER_AUDIO_PATH\""
            }
        if (raw == rewritten) return response
        val node = runCatching { objectMapper.readTree(rewritten) }.getOrElse { return response }
        return response.copy(interactiveGraph = node)
    }

    companion object {
        const val DIGITAL_SURVIVAL_STORY_OWNER = "seed:digital-survival-guide-v1"
        const val CDN_DIGITAL_SURVIVAL_PREFIX = "https://cdn.tamixa.app/library/sim/digital-survival-guide/"
        /** Resolved by mobile/web against API base (see ApiConfig.resolveAudioUrl). */
        const val PLACEHOLDER_AUDIO_PATH = "/api/v1/dev/digital-survival/placeholder.mp3"
    }
}
