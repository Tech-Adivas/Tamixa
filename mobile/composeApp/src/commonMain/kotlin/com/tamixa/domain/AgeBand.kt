package com.tamixa.domain

/**
 * Listener age bands for the 5–10 product focus. A library story belongs to a band when its target [Story.age]
 * falls inside it (admin "Age group" stores the band's first year, e.g. 7 for 7–8). No band selected = all stories.
 */
enum class AgeBand(val key: String, val minAge: Int, val maxAge: Int) {
    AGES_5_6("5-6", 5, 6),
    AGES_7_8("7-8", 7, 8),
    AGES_9_10("9-10", 9, 10);

    fun includes(age: Int): Boolean = age in minAge..maxAge

    companion object {
        /** Youngest and oldest ages the app targets; story generation keeps ages inside this range. */
        const val FOCUS_MIN_AGE = 5
        const val FOCUS_MAX_AGE = 10

        /** Band for a stored preference key; null (all ages) for blank or unknown keys. */
        fun fromKey(key: String?): AgeBand? = entries.firstOrNull { it.key == key?.trim() }
    }
}

/** Stories for [band]; every story when [band] is null. */
fun List<Story>.filterByAgeBand(band: AgeBand?): List<Story> =
    if (band == null) this else filter { band.includes(it.age) }
