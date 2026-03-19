package com.tamixa.application.port

import com.tamixa.domain.RevenueSnapshot
import java.time.LocalDate

interface RevenueSnapshotPort {
    fun save(snapshot: RevenueSnapshot): RevenueSnapshot
    fun findByDate(date: LocalDate): RevenueSnapshot?
}
