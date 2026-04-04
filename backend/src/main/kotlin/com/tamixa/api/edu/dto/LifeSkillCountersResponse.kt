package com.tamixa.api.edu.dto

/**
 * Raw soft counters for internal tooling / future dashboard. Not grades or exams.
 */
data class LifeSkillCountersResponse(
    val childId: Long,
    val wisdom: Int,
    val social: Int,
    val money: Int,
    val balance: Int,
    /** Product/legal: no “skill percentages” or certificates until reviewed. */
    val copyForParents: String =
        "These are private practice signals from story choices—not scores or school grades.",
)
