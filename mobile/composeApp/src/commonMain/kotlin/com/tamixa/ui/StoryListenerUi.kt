package com.tamixa.ui

import com.tamixa.domain.Story
import com.tamixa.util.TamixaConstants

private val funStoryLabels = setOf("fun stories", "funny stories")

/** Lighter / classic lane — match admin categories "Fun stories" or "Funny Stories". */
fun isFunStory(theme: String, category: String?): Boolean {
    val c = category?.trim()?.lowercase().orEmpty()
    val t = theme.trim().lowercase()
    return c in funStoryLabels || t in funStoryLabels
}

fun isFunStory(story: Story): Boolean = isFunStory(story.theme, story.category)

/** Legacy "Learn ·" hub rows; prefer [isFunStory] for badges. */
private val learnPrefix = Regex("^learn\\b", RegexOption.IGNORE_CASE)

fun isLearnStory(theme: String, category: String?): Boolean {
    val c = category?.trim().orEmpty()
    val t = theme.trim()
    return learnPrefix.containsMatchIn(c) || learnPrefix.containsMatchIn(t)
}

fun isLearnStory(story: Story): Boolean = isLearnStory(story.theme, story.category)

/** Edu & safety lane: Learn-prefixed metadata or explicit Digital Safety series. */
fun isLearnOrDigitalSafetyStory(story: Story): Boolean {
    if (isLearnStory(story)) return true
    val needle = "digital safety"
    return story.theme.contains(needle, ignoreCase = true) ||
        story.category?.contains(needle, ignoreCase = true) == true
}

/** Interactive life-simulator series (branching); theme/category starts with "Learn · Simulator". */
private val simulatorPrefix = Regex("^learn\\s*·\\s*simulator\\b", RegexOption.IGNORE_CASE)

fun isSimulatorStory(theme: String, category: String?): Boolean {
    val c = category?.trim().orEmpty()
    val t = theme.trim()
    return simulatorPrefix.containsMatchIn(c) || simulatorPrefix.containsMatchIn(t)
}

fun isSimulatorStory(story: Story): Boolean = isSimulatorStory(story.theme, story.category)

/** Library “Practice” hub: naming convention or non-empty interactive graph from API. */
fun isInteractivePracticeLibraryStory(story: Story): Boolean {
    if (isSimulatorStory(story)) return true
    val g = story.interactiveGraph
    return g != null && g.isNotEmpty()
}

private const val MAX_CONTEXT = 120

private fun truncate(s: String): String =
    if (s.length > MAX_CONTEXT) s.take(MAX_CONTEXT) + "…" else s

/** Optional subtitle under the player title while listening. */
fun listenerPlaybackSubtitle(story: Story, storySource: String): String? {
    val theme = story.theme.trim()
    val category = story.category?.trim().orEmpty()
    val title = story.title?.trim().orEmpty()
    return when (storySource) {
        TamixaConstants.STORY_SOURCE_LIBRARY -> {
            if (isFunStory(story)) {
                val focus = category.takeIf { it.isNotBlank() } ?: theme.takeIf { it.isNotBlank() } ?: "Just for fun"
                truncate(focus)
            } else if (theme.isNotBlank() && title.isNotBlank() && theme != title) {
                truncate(theme)
            } else if (category.isNotBlank()) {
                truncate(category)
            } else {
                null
            }
        }
        else -> {
            if (theme.isBlank()) null
            else if (title.isNotBlank() && theme == title) null
            else truncate(theme)
        }
    }
}
