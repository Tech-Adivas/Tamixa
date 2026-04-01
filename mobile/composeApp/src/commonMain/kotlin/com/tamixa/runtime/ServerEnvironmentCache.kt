package com.tamixa.runtime

/**
 * Mirrors persisted subscription URL override for synchronous reads in [com.tamixa.platform.getSubscriptionWebUrl].
 * Updated on app start and when the user saves server settings.
 */
object ServerEnvironmentCache {
    /** Not @Volatile: common code targets non-JVM (iOS); writes occur on save / startup. */
    var subscriptionWebUrlOverride: String = ""

    fun effectiveSubscriptionWebUrl(buildDefault: String): String {
        val o = subscriptionWebUrlOverride.trim()
        return if (o.isNotEmpty()) o else buildDefault.trim()
    }
}
