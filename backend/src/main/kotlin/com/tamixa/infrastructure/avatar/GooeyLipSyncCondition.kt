package com.tamixa.infrastructure.avatar

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Enables Gooey lip-sync bean when app.avatar-video.enabled=true and
 * app.avatar-video.provider is "gooey" (case-insensitive), with GOOEY_API_KEY and GOOEY_RECIPE_ID set.
 */
class GooeyLipSyncCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env: Environment = context.environment
        val enabledStr = env.getProperty("app.avatar-video.enabled")
            ?: env.getProperty("AVATAR_VIDEO_ENABLED", "false")
        val enabled = enabledStr.trim().lowercase() == "true"
        val providerStr = env.getProperty("app.avatar-video.provider")
            ?: env.getProperty("AVATAR_VIDEO_PROVIDER", "sadtalker")
        val provider = providerStr.trim().lowercase()
        val apiKey = (env.getProperty("app.avatar-video.gooey-api-key")
            ?: env.getProperty("GOOEY_API_KEY") ?: "").trim()
        val recipeId = (env.getProperty("app.avatar-video.gooey-recipe-id")
            ?: env.getProperty("GOOEY_RECIPE_ID") ?: "").trim()

        if (!enabled) {
            log.debug("Gooey LipSync: disabled (app.avatar-video.enabled != true)")
            return false
        }
        if (provider != "gooey") {
            log.debug("Gooey LipSync: not active (provider='{}')", provider.ifEmpty { "<empty>" })
            return false
        }
        if (apiKey.isEmpty()) {
            log.warn("Gooey LipSync: provider=gooey but GOOEY_API_KEY is empty. Set it in .env and restart.")
            return false
        }
        if (recipeId.isEmpty()) {
            log.warn("Gooey LipSync: GOOEY_RECIPE_ID is empty. Create a lip-sync recipe at gooey.ai and set GOOEY_RECIPE_ID in .env.")
            return false
        }
        log.info("Gooey LipSync: enabled (recipe_id set). Avatar video will use Gooey API.")
        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(GooeyLipSyncCondition::class.java)
    }
}
