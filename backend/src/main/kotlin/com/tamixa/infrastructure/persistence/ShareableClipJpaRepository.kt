package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface ShareableClipJpaRepository : JpaRepository<ShareableClipEntity, Long> {

    fun findByParent_IdAndCreatedAtAfter(parentId: Long, since: Instant): List<ShareableClipEntity>

    fun findByIdAndParent_Id(id: Long, parentId: Long): ShareableClipEntity?

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "UPDATE ShareableClipEntity c SET c.downloadAnalyticsEmittedAt = :now WHERE c.id = :id AND c.downloadAnalyticsEmittedAt IS NULL"
    )
    fun markDownloadAnalyticsEmittedIfUnset(@org.springframework.data.repository.query.Param("id") id: Long, @org.springframework.data.repository.query.Param("now") now: Instant): Int
}
