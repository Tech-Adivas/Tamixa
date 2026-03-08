package com.araro.application.feedback

import com.araro.infrastructure.persistence.ParentJpaRepository
import com.araro.infrastructure.persistence.StoryFeedbackEntity
import com.araro.infrastructure.persistence.StoryFeedbackJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeedbackService(
    private val parentJpaRepository: ParentJpaRepository,
    private val feedbackJpaRepository: StoryFeedbackJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun submit(parentEmail: String, storyId: Long?, storySource: String?, rating: Int?, comment: String?) {
        val parent = parentJpaRepository.findByEmail(parentEmail) ?: throw com.araro.application.child.ParentNotFoundException(parentEmail)
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
