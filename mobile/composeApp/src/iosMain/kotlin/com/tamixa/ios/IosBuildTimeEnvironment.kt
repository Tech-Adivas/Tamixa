package com.tamixa.ios

/**
 * Defaults baked in at iOS app build time (from Info.plist via Swift).
 * Used by [com.tamixa.platform.getSubscriptionWebUrl] before user overrides in Settings.
 */
internal object IosBuildTimeEnvironment {
    var defaultSubscriptionWebUrl: String = "https://app.tamixa.com/subscription"
    /** dev | qa | prod — mirrors Android [BuildConfig.TAMIXA_ENVIRONMENT]. */
    var label: String = "prod"
}
