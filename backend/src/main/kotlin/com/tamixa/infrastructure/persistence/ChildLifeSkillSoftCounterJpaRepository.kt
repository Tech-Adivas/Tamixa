package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChildLifeSkillSoftCounterJpaRepository : JpaRepository<ChildLifeSkillSoftCounterEntity, Long>
