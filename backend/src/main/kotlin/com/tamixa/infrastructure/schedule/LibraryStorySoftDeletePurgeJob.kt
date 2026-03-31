package com.tamixa.infrastructure.schedule

import com.tamixa.application.storylibrary.StoryLibraryService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Permanently deletes library stories that were soft-deleted longer than [AppProperties.libraryStorySoftDelete.retentionDays].
 */
@Component
@ConditionalOnProperty(
    name = ["app.library-story-soft-delete.purge-enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class LibraryStorySoftDeletePurgeJob(
    private val storyLibraryService: StoryLibraryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.library-story-soft-delete.purge-cron:0 0 4 * * *}")
    @SchedulerLock(name = "LibraryStorySoftDeletePurgeJob.purgeExpiredSoftDeletes", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30S")
    fun purgeExpiredSoftDeletes() {
        val removed = storyLibraryService.permanentlyPurgeExpiredSoftDeletes()
        if (removed > 0) {
            log.info("Library story soft-delete purge completed: {} row(s) permanently removed", removed)
        }
    }
}
