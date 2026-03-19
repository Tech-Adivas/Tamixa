package com.tamixa.infrastructure.avatar

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.core.env.Environment

/**
 * Enables Replicate (SadTalker) bean when app.avatar-video.enabled=true and
 * app.avatar-video.provider is "sadtalker" (case-insensitive, trimmed).
 * Uses Environment so we are not dependent on annotation placeholder resolution.
 */
class ReplicateSadTalkerCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env: Environment = context.environment
        // Prefer bound property; fallback to env var (e.g. when .env is loaded by Gradle)
        val enabledStr = env.getProperty("app.avatar-video.enabled")
            ?: env.getProperty("AVATAR_VIDEO_ENABLED", "false")
        val enabled = enabledStr.trim().lowercase() == "true"
        val providerStr = env.getProperty("app.avatar-video.provider")
            ?: env.getProperty("AVATAR_VIDEO_PROVIDER", "sadtalker")
        val provider = providerStr.trim().lowercase()
        val token = (env.getProperty("app.avatar-video.replicate-api-token")
            ?: env.getProperty("REPLICATE_API_TOKEN") ?: "").trim()

        if (!enabled) {
            log.debug("Replicate SadTalker: disabled (app.avatar-video.enabled != true)")
            return false
        }
        if (provider != "sadtalker") {
            log.info("Replicate SadTalker: not active (app.avatar-video.provider='{}', need 'sadtalker')", provider.ifEmpty { "<empty>" })
            return false
        }
        if (token.isEmpty()) {
            log.warn("Replicate SadTalker: provider=sadtalker but REPLICATE_API_TOKEN is empty; bean will be created but createPrediction will no-op. Set REPLICATE_API_TOKEN in .env and restart.")
        } else {
            log.debug("Replicate SadTalker: enabled (provider=sadtalker, token set). Avatar video will use Replicate API.")
        }
        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(ReplicateSadTalkerCondition::class.java)
    }
}
