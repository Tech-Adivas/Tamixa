package com.tamixa.application.port

import com.tamixa.domain.ReadingStreak

interface ReadingStreakRepositoryPort {
    fun findByChildId(childId: Long): ReadingStreak?
    fun save(streak: ReadingStreak): ReadingStreak
}
