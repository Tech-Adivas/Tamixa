package com.araro.domain

import java.time.Instant
import java.time.LocalDate

data class Child(
    val id: Long,
    val parentId: Long,
    val name: String,
    val dateOfBirth: LocalDate,
    val languagePreference: String?,
    /** Phase 2: Interests/preferences for personalization (e.g. "dinosaurs, space, animals"). */
    val interests: String? = null,
    val createdAt: Instant,
    /** Character builder: favorite color for story personalization */
    val favoriteColor: String? = null,
    /** Character builder: favorite animal */
    val favoriteAnimal: String? = null,
    /** Character builder: traits (e.g. "brave, curious, kind") */
    val characterTraits: String? = null,
    /** Character builder: avatar choice (e.g. "dragon", "unicorn") */
    val avatarChoice: String? = null
) {
    fun ageInYears(): Int {
        return java.time.temporal.ChronoUnit.YEARS.between(dateOfBirth, LocalDate.now()).toInt()
    }
}
