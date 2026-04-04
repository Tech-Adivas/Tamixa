package com.tamixa.application.edu

import com.tamixa.infrastructure.persistence.LifeSkillChoiceAggregateRow
import com.tamixa.infrastructure.persistence.LifeSkillChoiceEventJpaRepository
import com.tamixa.infrastructure.persistence.LibraryStoryEntity
import com.tamixa.infrastructure.persistence.LibraryStoryJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class LifeSkillChoiceAnalyticsServiceTest {

    @Mock
    private lateinit var lifeSkillChoiceEventJpaRepository: LifeSkillChoiceEventJpaRepository

    @Mock
    private lateinit var libraryStoryJpaRepository: LibraryStoryJpaRepository

    @InjectMocks
    private lateinit var service: LifeSkillChoiceAnalyticsService

    @Test
    fun `getAggregates maps rows and resolves titles`() {
        val row =
            object : LifeSkillChoiceAggregateRow {
                override fun getLibraryStoryId(): Long = 42L

                override fun getSegmentId(): String = "intro"

                override fun getChoiceId(): String = "verify_caller"

                override fun getEventCount(): Long = 7L
            }
        whenever(lifeSkillChoiceEventJpaRepository.countByCreatedAtAfter(any()))
            .thenReturn(7L)
        whenever(lifeSkillChoiceEventJpaRepository.aggregateSince(any()))
            .thenReturn(listOf(row))
        val story = LibraryStoryEntity(id = 42L, content = "x", theme = "Learn · Simulator · Digital Safety", age = 8)
        story.title = "Scam theater pilot"
        whenever(libraryStoryJpaRepository.findAllById(listOf(42L))).thenReturn(listOf(story))

        val out = service.getAggregates(30)

        assertThat(out.periodDays).isEqualTo(30)
        assertThat(out.totalEvents).isEqualTo(7L)
        assertThat(out.rows).hasSize(1)
        assertThat(out.rows[0].libraryStoryId).isEqualTo(42L)
        assertThat(out.rows[0].storyTitle).isEqualTo("Scam theater pilot")
        assertThat(out.rows[0].segmentId).isEqualTo("intro")
        assertThat(out.rows[0].choiceId).isEqualTo("verify_caller")
        assertThat(out.rows[0].eventCount).isEqualTo(7L)
    }
}
