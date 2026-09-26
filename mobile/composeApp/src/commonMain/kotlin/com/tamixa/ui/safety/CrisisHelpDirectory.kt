package com.tamixa.ui.safety

/**
 * Verified public helplines and portals (India-oriented). Shown in-app for crisis routing only.
 * Localized titles and copy live in [com.tamixa.ui.strings.Strings]; URLs and dial digits stay here via [CrisisHelpline].
 * Tamixa is not affiliated with these agencies; numbers/URLs may change—verify independently.
 */
data class CrisisHelpline(
    val title: String,
    val subtitle: String?,
    val websiteUrl: String?,
    /** Digits for [tel:] URI (non-digits stripped when dialing). */
    val phoneNumbers: List<String> = emptyList(),
    /** Opens default SMS app to this number (no body). */
    val smsNumber: String? = null,
    val note: String? = null,
)

data class CrisisHelplineSection(
    val sectionTitle: String,
    val lines: List<CrisisHelpline>,
)
