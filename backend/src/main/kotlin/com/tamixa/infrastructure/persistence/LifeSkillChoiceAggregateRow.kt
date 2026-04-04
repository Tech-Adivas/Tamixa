package com.tamixa.infrastructure.persistence

/**
 * Spring Data JPA projection for [LifeSkillChoiceEventJpaRepository.aggregateSince].
 */
interface LifeSkillChoiceAggregateRow {
    fun getLibraryStoryId(): Long
    fun getSegmentId(): String
    fun getChoiceId(): String
    fun getEventCount(): Long
}
