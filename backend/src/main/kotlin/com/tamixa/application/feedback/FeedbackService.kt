package com.tamixa.application.feedback

import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.StoryFeedbackEntity
import com.tamixa.infrastructure.persistence.StoryFeedbackJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeedbackService(
    private val parentJpaRepository: ParentJpaRepository,
    private val feedbackJpaRepository: StoryFeedbackJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun submit(parentEmail: String, storyId: Long?, storySource: String?, rating: Int?, comment: String?) {
        val parent = parentJpaRepository.findByEmail(parentEmail) ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        log.debug("Feedback submit parentId={} storyId={} rating={}", parent.id, storyId, rating)
        val entity = StoryFeedbackEntity(
            parent = parent,
            storyId = storyId,
            storySource = storySource ?: "generated",
            rating = rating?.coerceIn(1, 5),
            comment = comment?.take(2000)
        )
        feedbackJpaRepository.save(entity)
    }
}
