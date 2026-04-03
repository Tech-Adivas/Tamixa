package com.tamixa.application.story

/**
 * Server-defined generation topics: safe theme strings that skip free-text child blocklist checks
 * when [com.tamixa.api.story.dto.GenerateStoryRequest.generationTopicId] is used.
 * Expand over time; keep IDs stable for clients.
 */
object StoryGenerationTopicRegistry {

    data class Entry(
        val id: String,
        /** Passed to the model as the story theme (Tamil-friendly phrasing). */
        val theme: String,
        val suggestedLearningFocus: String? = null,
        val descriptionEn: String? = null,
    )

    private val byId: Map<String, Entry> = listOf(
        Entry(
            id = "teachers_day",
            theme = "ஆசிரியர் தினம் — ஏன் கொண்டாடுகிறோம்; ஒரு ஆசிரியரின் மரியாதை",
            suggestedLearningFocus = "kindness",
            descriptionEn = "Teachers' Day — gratitude and respect",
        ),
        Entry(
            id = "pongal_gratitude",
            theme = "பொங்கல் — நன்றியுணர்வும் புதிய தொடக்கமும்",
            suggestedLearningFocus = "curiosity",
            descriptionEn = "Pongal — gratitude and harvest",
        ),
        Entry(
            id = "tn_heritage_intro",
            theme = "தமிழ்நாட்டின் பாரம்பரியம் — மொழி, கலை, இயற்கை நேசிப்பு (குழந்தைகளுக்கு எளிய கதை)",
            suggestedLearningFocus = "curiosity",
            descriptionEn = "Tamil Nadu heritage — language, arts, nature (kid-friendly)",
        ),
        Entry(
            id = "constitution_day_simple",
            theme = "சட்டம் நம்மை எப்படி காக்கிறது — எளிய குடிமைப் பாடம் (கதை வடிவில், அரசியல் இல்லை)",
            suggestedLearningFocus = "responsibility",
            descriptionEn = "Simple civics — rules keep us fair (story, non-partisan)",
        ),
        Entry(
            id = "money_saving_goal",
            theme = "சிறிய சேமிப்பு இலக்கு — நேர்மையாக சேர்த்தல்",
            suggestedLearningFocus = "money_literacy",
            descriptionEn = "Saving toward a small honest goal",
        ),
        Entry(
            id = "public_speaking_club",
            theme = "பள்ளியில் சிறு பேச்சு — மூச்சு எடுத்து தெளிவாக சொல்லுதல்",
            suggestedLearningFocus = "public_speaking",
            descriptionEn = "Short classroom talk — clear and brave",
        ),
        Entry(
            id = "two_sources_truth",
            theme = "இரண்டு ஆதாரங்கள் — ஒரு செய்தியை எப்படி சரிபார்ப்பது",
            suggestedLearningFocus = "research_skills",
            descriptionEn = "Checking a fact with two sources",
        ),
    ).associateBy { it.id }

    fun all(): List<Entry> = byId.values.sortedBy { it.id }

    fun resolve(id: String): Entry? = byId[id.trim().lowercase()]
}
