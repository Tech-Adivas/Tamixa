package com.tamixa.application.port.visuals

/** LLM used to plan a story's slideshow: returns the assistant's JSON reply to the conversation, or null. */
interface StoryVisualPlannerLlmPort {

    fun completeJson(messages: List<PlannerMessage>): String?
}

data class PlannerMessage(val role: String, val content: String)
