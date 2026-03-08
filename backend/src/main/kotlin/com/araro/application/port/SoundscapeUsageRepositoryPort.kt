package com.araro.application.port

import com.araro.domain.SoundscapeUsage

interface SoundscapeUsageRepositoryPort {
    fun save(usage: SoundscapeUsage): SoundscapeUsage
    fun findByParentId(parentId: Long): List<SoundscapeUsage>
    fun findByStoryId(storyId: Long): List<SoundscapeUsage>
    fun findByParentIdAndSoundscapeId(parentId: Long, soundscapeId: Long): SoundscapeUsage?
    fun incrementUsage(parentId: Long, storyId: Long?, soundscapeId: Long): SoundscapeUsage
}
