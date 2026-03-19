package com.tamixa.application.story

/**
 * Result of content moderation. Used to block political, violent, or toxic content.
 */
data class ModerationResult(
    val safe: Boolean,
    val categories: ModerationCategories = ModerationCategories()
) {
    fun withCategories(
        hate: Boolean = false,
        hateThreatening: Boolean = false,
        harassment: Boolean = false,
        selfHarm: Boolean = false,
        sexual: Boolean = false,
        sexualMinors: Boolean = false,
        violence: Boolean = false,
        violenceGraphic: Boolean = false
    ): ModerationResult = copy(
        categories = ModerationCategories(
            hate = hate,
            hateThreatening = hateThreatening,
            harassment = harassment,
            selfHarm = selfHarm,
            sexual = sexual,
            sexualMinors = sexualMinors,
            violence = violence,
            violenceGraphic = violenceGraphic
        ),
        safe = !(hate || hateThreatening || harassment || selfHarm || sexual || sexualMinors || violence || violenceGraphic)
    )
}

data class ModerationCategories(
    val hate: Boolean = false,
    val hateThreatening: Boolean = false,
    val harassment: Boolean = false,
    val selfHarm: Boolean = false,
    val sexual: Boolean = false,
    val sexualMinors: Boolean = false,
    val violence: Boolean = false,
    val violenceGraphic: Boolean = false
) {
    /** Political content is not in OpenAI categories; we block via blocklist in safety middleware. */
    fun anyFlagged(): Boolean =
        hate || hateThreatening || harassment || selfHarm || sexual || sexualMinors || violence || violenceGraphic
}
