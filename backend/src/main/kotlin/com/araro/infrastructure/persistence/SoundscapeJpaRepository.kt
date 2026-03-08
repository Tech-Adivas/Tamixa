package com.araro.infrastructure.persistence

import com.araro.domain.SoundscapeCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SoundscapeJpaRepository : JpaRepository<SoundscapeEntity, Long> {
    fun findByCategory(category: SoundscapeCategory): List<SoundscapeEntity>
    fun findByNameContaining(name: String): List<SoundscapeEntity>
}
