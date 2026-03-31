package com.tamixa.application.story

/**
 * Canonical TTS storytelling spec for admin "Regenerate with prompt" / conversion.
 * Primary language is substituted; model must still return the usual JSON contract (see [StoryPromptBuilder.buildDefaultConversionPrompt]).
 */
internal object AdminTtsStoryConversionPromptBody {

    fun build(primaryLanguageCode: String): String = """
Create a storytelling script for Text-to-Speech (TTS) audio generation.

IMPORTANT — You are REWRITING the user's existing story (supplied after this block). Preserve plot, characters, emotional arc, and moral intent. Do not replace it with an unrelated new tale unless the source is empty; strengthen continuity, pacing, and spoken delivery per the rules below.

================================
LANGUAGE CONFIGURATION
================================
Primary language: $primaryLanguageCode

For THIS response: write title, moral, and story_text entirely in the primary language only. Do not mix languages. Other locales may be produced by a separate pipeline; do not embed multiple languages in one story_text.

================================
TARGET AUDIENCE
================================
- Suitable for all age groups (children, teens, adults, elders)
- Must be child-safe but engaging for older audiences as well
- Tone should balance simplicity and emotional depth
- Should feel like a universal story (cartoon-style appeal)

================================
AUDIO REQUIREMENTS
================================
- Target TTS duration: around 7 to 8 minutes when read aloud at natural pacing
- Target full-length: aim for roughly ${StoryPromptTemplates.FULL_LENGTH_WORDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_WORDS_MAX} words (about 7–8 minutes at natural pacing)
- Use natural pauses and spoken-friendly pacing
- Avoid overly complex or dense sentences
- Ensure smooth narration flow for listening

================================
STORY FORMAT
================================
Output must include:
1. Title
2. Moral
3. Full storytelling script with voice modulation tags

================================
STORY REQUIREMENTS
================================
- Strengthen or clarify from source: fully satisfying arc (originality of voice and delivery, not necessarily wholly new plot if source exists)
- Theme must be generic and flexible (not restricted to a specific setting or character type)
- Story should be relatable across ages
- Maintain strong beginning, middle, and end
- Include emotional depth for adults and clarity for children
- Use natural dialogue where appropriate

================================
NARRATIVE CONTINUITY (CRITICAL)
================================
- There must be NO discontinuity, NO abrupt jumps, and NO gaps in the story
- Every scene must connect smoothly to the next
- Maintain logical progression of actions and emotions
- Ensure a complete and satisfying ending

================================
VOICE MODULATION TAGS (MANDATORY)
================================
Use throughout:

Tone:
[Warm tone], [Gentle tone], [Calm tone], [Happy tone], [Excited tone], [Playful tone], [Curious tone], [Wonder tone], [Reassuring tone], [Thoughtful tone], [Soft voice], [Whispered tone], [Emotional tone], [Soft emotional tone], [Celebration tone], [Storyteller tone]

Pacing:
[Slow pacing], [Medium pacing], [Brisk pacing]

Pauses:
[Pause 300ms], [Pause 500ms], [Pause 700ms], [Pause 1s], [Pause 1.5s]

Scene flow:
[Scene opens softly], [Scene shifts], [A gentle moment], [A magical moment], [A quiet pause], [A joyful moment], [A surprise moment], [Closing tone]

Rules:
- Use tags meaningfully, not excessively
- Add pauses after emotional or visual moments
- Maintain expressive narration for TTS

================================
DIALOGUE RULES
================================
- Use natural, simple, and expressive dialogue
- Keep it understandable for children but meaningful for adults
- Avoid overly long speeches

================================
HABIT-BUILDING DESIGN
================================
- Include 1 to 3 positive habits naturally

Rules:
- Show habits through actions, not instructions
- Use pattern: situation → action → result → feeling → learning
- Reinforce behaviors subtly across scenes
- Avoid preachy tone

================================
MICRO-BEHAVIOR DESIGN
================================
- Show small, realistic actions
- Repeat behaviors naturally across the story

================================
EMOTION LOOP
================================
Use:
Action → Feeling → Response → Memory

- Ensure emotional connection for all age groups

================================
MEMORY ANCHORS
================================
- Include a recurring element (object/action/phrase)
- Repeat 2–3 times with emotional significance

================================
DECISION MOMENTS
================================
- Include 1–2 simple decisions
- Show thinking → choice → outcome

================================
AUDIO IMAGINATION
================================
- Include sound-based descriptions
- Make scenes vivid for listeners

================================
COGNITIVE SIMPLICITY + DEPTH
================================
- Keep structure simple for children
- Add emotional layers for older audiences
- Avoid confusion or overload
================================
EDUCATIONAL PERSPECTIVE (Tamixa differentiator)
================================
- Build learning naturally into the story (do not lecture).
- SEL: include 1–2 moments where characters name their feelings (e.g. happy, worried, brave, kind, proud, grateful) in the primary language.
- Wonder words: weave in 2–4 age-appropriate rich words used clearly in context. Repeat each of those words at least twice so the listener can infer meaning without a glossary.
- Inference-friendly narrative: include at least one moment that rewards paying attention (a choice that makes sense based on what happened earlier, or a gentle “what might happen next?” beat). Do not add quizzes.
- One clear problem, one satisfying resolution: include a single main challenge, and resolve it in a way that feels earned (character effort, help from others, or a lesson learned). Keep plot continuity from the source.
- NEP / life skills alignment: support critical thinking (characters weighing options), collaboration (helping each other), cultural awareness (respect, diversity), and values (honesty, sharing, courage) implicitly in the story, not preachy.

================================
SELF-IDENTIFICATION
================================
- Characters should feel relatable to different age groups
- Include simple thoughts and emotional reflections

================================
VALUES TO PROMOTE
================================
- kindness, empathy, compassion
- cooperation, togetherness, collaboration
- respect and inclusion
- friendship and relationships
- curiosity and learning
- emotional safety and understanding
- appreciation of differences
- hope and encouragement
- peaceful problem-solving
- responsibility and mindful decisions
- sharing and helping others
- simple money awareness (saving, valuing, avoiding waste)
- gratitude
- positive resolution

Rule:
- Values must be shown, not told

================================
SAFETY & COMPLIANCE
================================
- Must be child-safe and inclusive
- No violence, abuse, or harmful content
- No discrimination or bias
- No political or religious persuasion
- No unsafe behavior imitation
- No personal or sensitive data

================================
FINAL GOAL
================================
Generate a storytelling script that:
- works for all age groups
- is emotionally engaging and immersive
- lasts about 7–8 minutes in TTS at natural pacing (roughly ${StoryPromptTemplates.FULL_LENGTH_WORDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_WORDS_MAX} words)
- has strong continuity and no gaps
- teaches values naturally through story
- feels like a universal, timeless narrative

The story should feel like an experience, not a lesson.
""".trimIndent()
}
