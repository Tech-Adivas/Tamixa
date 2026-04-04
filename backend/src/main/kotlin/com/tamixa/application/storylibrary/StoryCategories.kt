package com.tamixa.application.storylibrary

/**
 * Canonical story categories aligned with the mobile app (SampleData.categories minus "All").
 * Used for bulk generation defaults, normalization, and validation so saved stories
 * always use the exact strings the app expects for filtering and display.
 *
 * "Learn · …" entries are topic lanes for editorial use (theme/category may start with "Learn").
 * "Learn · Simulator · …" marks interactive branching life-simulator content (see docs/admin/EDU_METADATA_CONVENTIONS.md).
 * "Fun stories" is the light / classic lane for tales curated mainly for laughs—move legacy rows here in admin.
 */
object StoryCategories {
    val canonical: List<String> = listOf(
        "Animals",
        "Friendship",
        "Adventure",
        "Village Life",
        "Moral Stories",
        "Fun stories",
        "Funny Stories",
        "Family Stories",
        "Fantasy",
        "Nature",
        "Bravery",
        "Learn · History",
        "Learn · Science & Nature",
        "Learn · Culture & Heritage",
        "Learn · Life Skills",
        "Learn · Digital Safety",
        "Learn · Simulator · Digital Safety",
    )

    /**
     * Maps an incoming category string (e.g. from API or LLM) to the canonical form
     * if it matches one of the canonical categories case-insensitively; otherwise returns the trimmed input.
     */
    fun toCanonical(input: String?): String? {
        val trimmed = input?.trim()?.take(100) ?: return null
        if (trimmed.isBlank()) return null
        return canonical.find { it.equals(trimmed, ignoreCase = true) } ?: trimmed
    }

    /**
     * Normalizes a list of category strings to canonical forms; drops blanks and invalid entries.
     */
    fun normalize(categories: List<String>): List<String> =
        categories.mapNotNull { toCanonical(it) }.distinct()
}
