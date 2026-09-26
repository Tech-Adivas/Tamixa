package com.tamixa.application.edu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LifeSkillPillarNormalizationTest {

    @Test
    fun `foldToPillars merges keys per pillar and ignores unknowns`() {
        val folded = LifeSkillPillarNormalization.foldToPillars(
            mapOf(
                "DIGITAL_WISDOM" to 3,
                "wisdom" to 2,
                "social_capital" to 4,
                "unknown_metric" to 99,
            ),
        )
        assertEquals(5, folded[LifeSkillPillar.WISDOM])
        assertEquals(4, folded[LifeSkillPillar.SOCIAL])
        assertNull(folded[LifeSkillPillar.MONEY])
    }

    @Test
    fun `pillarForDeltaKey maps fiscal and cognitive aliases`() {
        assertEquals(LifeSkillPillar.MONEY, LifeSkillPillarNormalization.pillarForDeltaKey("fiscal_muscle"))
        assertEquals(LifeSkillPillar.BALANCE, LifeSkillPillarNormalization.pillarForDeltaKey("cognitive_agency"))
    }

    @Test
    fun `pillarForDeltaKey maps interactive story author keys`() {
        assertEquals(LifeSkillPillar.WISDOM, LifeSkillPillarNormalization.pillarForDeltaKey("integrity"))
        assertEquals(LifeSkillPillar.SOCIAL, LifeSkillPillarNormalization.pillarForDeltaKey("leadership"))
        assertEquals(LifeSkillPillar.BALANCE, LifeSkillPillarNormalization.pillarForDeltaKey("harmony"))
        assertEquals(LifeSkillPillar.MONEY, LifeSkillPillarNormalization.pillarForDeltaKey("wallet"))
        assertEquals(LifeSkillPillar.MONEY, LifeSkillPillarNormalization.pillarForDeltaKey("business_health"))
    }
}
