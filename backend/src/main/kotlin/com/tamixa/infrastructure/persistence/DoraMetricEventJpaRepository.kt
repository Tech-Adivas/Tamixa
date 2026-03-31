package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface DoraMetricEventJpaRepository : JpaRepository<DoraMetricEventEntity, Long> {
    @Query(
        """
        select e from DoraMetricEventEntity e
        where e.createdAt >= :windowStart
          and (:serviceName is null or e.serviceName = :serviceName)
          and (:environment is null or e.environment = :environment)
        """
    )
    fun findForWindow(
        @Param("windowStart") windowStart: Instant,
        @Param("serviceName") serviceName: String?,
        @Param("environment") environment: String?
    ): List<DoraMetricEventEntity>
}
