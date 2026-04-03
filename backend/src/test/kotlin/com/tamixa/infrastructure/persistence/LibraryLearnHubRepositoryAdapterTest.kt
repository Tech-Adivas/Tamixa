package com.tamixa.infrastructure.persistence

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

/**
 * Ensures Learn-hub list paths delegate to the JPA methods used by [com.tamixa.application.storylibrary.StoryLibraryService.findByLanguageApprovedOnly].
 */
@ExtendWith(MockitoExtension::class)
class StoryLibraryRepositoryAdapterLearnHubTest {

    @Mock
    lateinit var libraryStoryJpaRepository: LibraryStoryJpaRepository

    @InjectMocks
    lateinit var adapter: StoryLibraryRepositoryAdapter

    @Test
    fun `delegates findByLanguageAndNarrationApprovedLearnPrefix to jpa`() {
        val pageable = PageRequest.of(0, 20)
        whenever(
            libraryStoryJpaRepository.findByLanguageAndNarrationApprovedLearnPrefix("ta", "Learn", pageable)
        ).thenReturn(PageImpl(emptyList()))

        adapter.findByLanguageAndNarrationApprovedLearnPrefix("ta", "Learn", pageable)

        verify(libraryStoryJpaRepository).findByLanguageAndNarrationApprovedLearnPrefix("ta", "Learn", pageable)
    }
}

@ExtendWith(MockitoExtension::class)
class StoryTranslationRepositoryAdapterLearnHubTest {

    @Mock
    lateinit var storyTranslationJpaRepository: StoryTranslationJpaRepository

    @InjectMocks
    lateinit var adapter: StoryTranslationRepositoryAdapter

    @Test
    fun `delegates findByLanguageAndMasterNarrationApprovedAndLearnPrefix to jpa`() {
        val pageable = PageRequest.of(0, 20)
        whenever(
            storyTranslationJpaRepository.findByLanguageAndMasterNarrationApprovedAndLearnPrefix("en", "Learn", pageable)
        ).thenReturn(PageImpl(emptyList()))

        adapter.findByLanguageAndMasterNarrationApprovedAndLearnPrefix("en", "Learn", pageable)

        verify(storyTranslationJpaRepository).findByLanguageAndMasterNarrationApprovedAndLearnPrefix("en", "Learn", pageable)
    }

    @Test
    fun `delegates findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix to jpa`() {
        val pageable = PageRequest.of(0, 20)
        whenever(
            storyTranslationJpaRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix("en", "Learn", pageable)
        ).thenReturn(PageImpl(emptyList()))

        adapter.findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix("en", "Learn", pageable)

        verify(storyTranslationJpaRepository).findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix("en", "Learn", pageable)
    }
}
