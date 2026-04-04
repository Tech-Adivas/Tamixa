package com.tamixa.api.admin.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LifeSkillChoiceAnalyticsRowResponse(
    val libraryStoryId: Long,
    val storyTitle: String?,
    val segmentId: String,
    val choiceId: String,
    val eventCount: Long,
)

data class LifeSkillChoiceAnalyticsResponse(
    val periodDays: Int,
    val totalEvents: Long,
    val rows: List<LifeSkillChoiceAnalyticsRowResponse>,
)
