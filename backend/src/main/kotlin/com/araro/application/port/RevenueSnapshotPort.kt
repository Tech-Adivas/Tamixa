package com.araro.application.port

import com.araro.domain.RevenueSnapshot
import java.time.LocalDate

interface RevenueSnapshotPort {
    fun save(snapshot: RevenueSnapshot): RevenueSnapshot
    fun findByDate(date: LocalDate): RevenueSnapshot?
}
