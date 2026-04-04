package com.tamixa.domain

import java.time.Instant

data class Story(
    val id: Long,
    val parentId: Long,
    val childId: Long?,
    val content: String,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val title: String? = null,
    val moral: String? = null,
    val status: StoryStatus = StoryStatus.PENDING,
    val audioFileUrl: String? = null,
    val createdAt: Instant,
    /** Safety score 0-100; stored for auditing. Rejected if below threshold. */
    val safetyScore: Int? = null,
    /** Phase 2: Voice cloning – parent's voice profile for TTS narration. */
    val voiceProfileId: Long? = null,
    /** Phase 2: CALM, SOOTHING, ADVENTUROUS, DEFAULT. Influences story tone (e.g. bedtime). */
    val emotionMode: String? = null,
    /** Phase 2: Sanitized parent instructions included in generation prompt. */
    val parentCustomPrompt: String? = null,
    /** AI-generated cover/illustration (GCS path or URL). */
    val coverImageUrl: String? = null,
    /** Animated GIF cover when image-to-video is configured. Path: generated_cover_videos/{id}.gif */
    val coverVideoUrl: String? = null
)
