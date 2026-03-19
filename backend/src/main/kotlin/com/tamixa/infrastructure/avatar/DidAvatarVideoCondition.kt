package com.tamixa.infrastructure.avatar

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Enables D-ID avatar video client when app.avatar-video.enabled=true and
 * app.avatar-video.provider is "d-id" (case-insensitive) and DID_API_KEY is set.
 */
class DidAvatarVideoCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env: Environment = context.environment
        val enabledStr = env.getProperty("app.avatar-video.enabled")
            ?: env.getProperty("AVATAR_VIDEO_ENABLED", "false")
        val enabled = enabledStr.trim().lowercase() == "true"
        val providerStr = env.getProperty("app.avatar-video.provider")
            ?: env.getProperty("AVATAR_VIDEO_PROVIDER", "sadtalker")
        val provider = providerStr.trim().lowercase()
        val apiKey = (env.getProperty("app.avatar-video.did-api-key")
            ?: env.getProperty("DID_API_KEY") ?: "").trim()

        if (!enabled) return false
        if (provider != "d-id") {
            log.debug("D-ID: not active (provider='{}')", provider.ifEmpty { "<empty>" })
            return false
        }
        if (apiKey.isEmpty()) {
            log.warn("D-ID: provider=d-id but DID_API_KEY is empty. Set DID_API_KEY in .env and restart.")
        } else {
            log.info("D-ID: enabled. Avatar video will use D-ID Talks API (V2 photo avatar + custom audio).")
        }
        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(DidAvatarVideoCondition::class.java)
    }
}
