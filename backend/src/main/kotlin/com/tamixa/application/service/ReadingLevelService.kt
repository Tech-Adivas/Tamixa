package com.tamixa.application.service

import com.tamixa.application.port.ReadingLevelRepositoryPort
import com.tamixa.domain.ReadingLevel
import com.tamixa.domain.ReadingLevelAssessment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ReadingLevelService(
    private val readingLevelRepository: ReadingLevelRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getOrCreateReadingLevel(childId: Long): ReadingLevel {
        return readingLevelRepository.findByChildId(childId)
            ?: run {
                log.debug("Creating default reading level for child={}", childId)
                readingLevelRepository.save(
                    ReadingLevel(
                        id = 0,
                        childId = childId,
                        level = ReadingLevel.DEFAULT_LEVEL,
                        updatedAt = Instant.now()
                    )
                )
            }
    }

    fun assessAndUpdateLevel(childId: Long, quizScore: Int, maxScore: Int, quizId: Long? = null): ReadingLevel {
        val current = getOrCreateReadingLevel(childId)
        val percentage = if (maxScore > 0) (quizScore * 100) / maxScore else 0

        val newLevel = when {
            percentage >= 90 && current.level < ReadingLevel.MAX_LEVEL -> current.level + 1
            percentage < 60 && current.level > ReadingLevel.MIN_LEVEL -> current.level - 1
            else -> current.level
        }

        if (newLevel != current.level) {
            log.info("Reading level changed for child={} from {} to {} (quiz score={}%)", childId, current.level, newLevel, percentage)
            readingLevelRepository.saveAssessment(
                ReadingLevelAssessment(
                    id = 0,
                    childId = childId,
                    quizId = quizId,
                    oldLevel = current.level,
                    newLevel = newLevel,
                    assessedAt = Instant.now()
                )
            )
        }

        return readingLevelRepository.save(
            current.copy(level = newLevel, updatedAt = Instant.now())
        )
    }

    fun getAssessmentHistory(childId: Long): List<ReadingLevelAssessment> {
        return readingLevelRepository.findAssessmentsByChildId(childId)
    }
}
