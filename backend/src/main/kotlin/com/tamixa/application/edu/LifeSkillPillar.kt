package com.tamixa.application.edu

/**
 * Canonical life-simulator pillars (beta). Author [skillDeltas] keys in the interactive graph map to these.
 * See docs/admin/EDU_METADATA_CONVENTIONS.md §7.
 */
enum class LifeSkillPillar {
    WISDOM,
    SOCIAL,
    MONEY,
    BALANCE,
}

object LifeSkillPillarNormalization {

    private val wisdomKeys = setOf(
        "wisdom",
        "digital_wisdom",
        "digitalwisdom",
        "wis",
    )
    private val socialKeys = setOf(
        "social",
        "social_capital",
        "socialcapital",
        "empathy",
        "negotiation",
    )
    private val moneyKeys = setOf(
        "money",
        "fiscal",
        "fiscal_muscle",
        "fiscalmuscle",
        "financial",
        "wallet",
    )
    private val balanceKeys = setOf(
        "balance",
        "cognitive",
        "cognitive_agency",
        "cognitiveagency",
        "focus",
        "research",
    )

    fun pillarForDeltaKey(rawKey: String): LifeSkillPillar? {
        val k = rawKey.trim().lowercase().replace('-', '_').replace(' ', '_')
        if (k.isBlank()) return null
        return when {
            k in wisdomKeys || k.startsWith("wisdom") || k.endsWith("_wisdom") -> LifeSkillPillar.WISDOM
            k.contains("integrity") || k.contains("clarity") || k == "skill" || k.endsWith("_skill") -> LifeSkillPillar.WISDOM
            k.contains("leadership") || k.contains("authority") || k.contains("status") -> LifeSkillPillar.SOCIAL
            k in socialKeys || k.contains("social") -> LifeSkillPillar.SOCIAL
            k in moneyKeys || k.contains("fiscal") || k.contains("money") || k.contains("debt") || k.contains("business_health") ->
                LifeSkillPillar.MONEY
            k in balanceKeys || k.contains("cognitive") || k.contains("balance") || k.contains("confidence") ||
                k.contains("harmony") || k == "risk" || k.contains("emotional") -> LifeSkillPillar.BALANCE
            else -> null
        }
    }

    /**
     * Merges raw author keys into the four pillars (single pass; keys mapping to same pillar sum).
     */
    fun foldToPillars(skillDeltas: Map<String, Int>): Map<LifeSkillPillar, Int> {
        val out = mutableMapOf<LifeSkillPillar, Int>()
        for ((raw, v) in skillDeltas) {
            val p = pillarForDeltaKey(raw) ?: continue
            out.merge(p, v.coerceIn(-50, 50), Int::plus)
        }
        return out
    }
}
