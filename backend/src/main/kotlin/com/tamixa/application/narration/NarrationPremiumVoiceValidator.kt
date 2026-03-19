package com.tamixa.application.narration

/**
 * Validates entitlement for premium (custom/cloned) voice profiles.
 * Default voice ("default") is always allowed.
 * Premium voices (e.g. custom:{id}) require subscription.voicePremium when parentId is present.
 */
interface NarrationPremiumVoiceValidator {

    /**
     * @param voiceProfile Voice identifier; "default" = built-in, always allowed
     * @param parentId Optional; when null (e.g. Kafka pipeline), only "default" is allowed
     * @return true if voice can be used
     */
    fun canUseVoice(voiceProfile: String, parentId: Long?): Boolean
}
