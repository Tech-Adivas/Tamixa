package com.araro.application.port

import com.araro.domain.Soundscape
import com.araro.domain.SoundscapeCategory

interface SoundscapeRepositoryPort {
    fun save(soundscape: Soundscape): Soundscape
    fun findById(id: Long): Soundscape?
    fun findAll(): List<Soundscape>
    fun getSoundscapesByCategory(category: SoundscapeCategory): List<Soundscape>
    fun searchByName(name: String): List<Soundscape>
}
