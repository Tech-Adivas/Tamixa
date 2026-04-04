package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "child_life_skill_soft_counters")
class ChildLifeSkillSoftCounterEntity(
    @Id
    @Column(name = "child_id")
    val childId: Long,

    @Column(nullable = false)
    var wisdom: Int = 0,

    @Column(nullable = false)
    var social: Int = 0,

    @Column(nullable = false)
    var money: Int = 0,

    @Column(nullable = false)
    var balance: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
