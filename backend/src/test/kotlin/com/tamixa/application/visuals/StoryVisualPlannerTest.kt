package com.tamixa.application.visuals

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tamixa.application.port.visuals.PlannerMessage
import com.tamixa.application.port.visuals.StoryVisualPlannerLlmPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Slideshow planning with a fake LLM: validation, retry with feedback, and filling long gaps between shots. */
class StoryVisualPlannerTest {

    private val mapper = jacksonObjectMapper()

    // 6 sentences of 10 words each = 60 words.
    private val story = (1..6).joinToString(" ") { s -> (1..10).joinToString(" ") { w -> "s${s}w$w" } + "." }
        .replace("s4w1 ", "\"s4w1 ")

    private class FakeLlm(private val replies: List<String>) : StoryVisualPlannerLlmPort {
        val conversations = mutableListOf<List<PlannerMessage>>()
        override fun completeJson(messages: List<PlannerMessage>): String? {
            conversations += messages.toList()
            return replies.getOrNull(conversations.size - 1)
        }
    }

    private fun plan(shots: String, images: String = IMAGES) = """
        {"characters":[{"name":"Mia","appearance":"5-year-old girl","outfits":{"day":"pink frock"}}],
         "images":$images,
         "timeline":$shots}
    """.trimIndent()

    private fun shot(image: String, start: String, focus: String? = null) =
        """{"image_id":"$image","narration_start":"$start","camera":"wide","focus":${focus?.let { "\"$it\"" } ?: "null"},"movement":"slow_zoom_in"}"""

    @Test
    fun `accepts a valid plan and matches start phrases whatever their quote style`() {
        val llm = FakeLlm(listOf(plan("[${shot("IMG_01", "s1w1 s1w2 s1w3 s1w4")}, ${shot("IMG_02", "'s4w1 s4w2 s4w3", "right")}]")))

        val result = StoryVisualPlanner(llm, mapper).plan(story, "ta", durationSeconds = 600)

        assertThat(llm.conversations).hasSize(1)
        assertThat(result.images.map { it.imageKey }).containsExactly("IMG_01", "IMG_02")
        assertThat(result.shots.first().startWord).isEqualTo(0)
        assertThat(result.shots.map { it.startWord }).contains(30)
        assertThat(result.shots.first { it.startWord == 30 }.focus).isEqualTo("right")
        assertThat(result.sourceWordCount).isEqualTo(60)
        assertThat(mapper.readTree(result.charactersJson)[0]["name"].asText()).isEqualTo("Mia")
    }

    @Test
    fun `sends problems back and uses the corrected plan`() {
        val bad = plan("[${shot("IMG_01", "s1w1 s1w2 s1w3")}, ${shot("IMG_02", "words that are not in the story")}]")
        val good = plan("[${shot("IMG_01", "s1w1 s1w2 s1w3")}, ${shot("IMG_02", "s3w1 s3w2 s3w3")}]")
        val llm = FakeLlm(listOf(bad, good))

        val result = StoryVisualPlanner(llm, mapper).plan(story, "ta", durationSeconds = 600)

        assertThat(llm.conversations).hasSize(2)
        assertThat(llm.conversations[1].last().content).contains("not copied exactly from the story")
        assertThat(result.shots.map { it.startWord }).contains(20)
    }

    @Test
    fun `fills a long passage with extra framings at sentence starts`() {
        // ~45 s of narration ≈ 9 words at 60 words / 300 s, so one shot for 60 words needs several more.
        val llm = FakeLlm(listOf(plan("[${shot("IMG_01", "s1w1 s1w2 s1w3")}]")))

        val result = StoryVisualPlanner(llm, mapper).plan(story, "ta", durationSeconds = 300)

        assertThat(result.shots.size).isGreaterThan(1)
        assertThat(result.shots.map { it.imageKey }.toSet()).containsExactly("IMG_01")
        assertThat(result.shots.drop(1).map { it.startWord }).allMatch { it % 10 == 0 }
        assertThat(result.shots.map { it.startWord }).isSorted
        assertThat(result.shots.drop(1).first().camera).isEqualTo("close_up")
        assertThat(result.shots.drop(1).first().focus).isEqualTo("left")
    }

    @Test
    fun `drops a shot that would follow the previous one too quickly`() {
        val planner = StoryVisualPlanner(FakeLlm(emptyList()), mapper)
        val shots = listOf(0, 3, 20).map { PlannedShot("IMG_01", "x", it, "wide", null, "none") }

        assertThat(planner.dropCrowded(shots, minShotWords = 8).map { it.startWord }).containsExactly(0, 20)
    }

    @Test
    fun `gives up after repeated bad plans`() {
        val noRegions = """[{"image_id":"IMG_01","story_moment":"x","image_prompt":"${"a long enough picture description ".repeat(3)}","subjects":[{"name":"Mia","region":"somewhere"}]}]"""
        val bad = plan("[${shot("IMG_01", "s1w1 s1w2 s1w3")}]", images = noRegions)
        val llm = FakeLlm(listOf(bad, bad, bad))

        assertThatThrownBy { StoryVisualPlanner(llm, mapper).plan(story, "ta", durationSeconds = 600) }
            .isInstanceOf(StoryVisualPlanException::class.java)
            .hasMessageContaining("region")
        assertThat(llm.conversations).hasSize(3)
    }

    private companion object {
        val IMAGES = """[
          {"image_id":"IMG_01","story_moment":"walk home","image_prompt":"${"Mia walks home with her mother in the rain. ".repeat(2)}","subjects":[{"name":"Mia","region":"left"},{"name":"Mother","region":"right"}]},
          {"image_id":"IMG_02","story_moment":"fish shop","image_prompt":"${"Mia looks at the blue fish in the shop tank. ".repeat(2)}","subjects":[{"name":"Mia","region":"left"},{"name":"Fin","region":"right"}]}
        ]"""
    }
}
