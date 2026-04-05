package com.tamixa.domain

/**
 * Five family-facing readiness dimensions (radar axes). Maps from [com.tamixa.network.LifeSkillCountersResponseDto]
 * the same way the web app maps offline [UserLifeProfile] — with ethics derived so the pentagon stays meaningful
 * when only four server pillars exist.
 */
enum class LifeReadinessAxisId {
    TECH,
    BUSINESS,
    LEADERSHIP,
    ETHICS,
    COMMUNICATION,
}

data class LifeReadinessSnapshot(
    val tech: Int,
    val business: Int,
    val leadership: Int,
    val ethics: Int,
    val communication: Int,
)

data class LifeReadinessAxisDef(
    val id: LifeReadinessAxisId,
)

val LIFE_READINESS_AXIS_ORDER: List<LifeReadinessAxisDef> = listOf(
    LifeReadinessAxisDef(LifeReadinessAxisId.TECH),
    LifeReadinessAxisDef(LifeReadinessAxisId.BUSINESS),
    LifeReadinessAxisDef(LifeReadinessAxisId.LEADERSHIP),
    LifeReadinessAxisDef(LifeReadinessAxisId.ETHICS),
    LifeReadinessAxisDef(LifeReadinessAxisId.COMMUNICATION),
)

fun valueForAxis(snapshot: LifeReadinessSnapshot, axis: LifeReadinessAxisId): Int =
    when (axis) {
        LifeReadinessAxisId.TECH -> snapshot.tech
        LifeReadinessAxisId.BUSINESS -> snapshot.business
        LifeReadinessAxisId.LEADERSHIP -> snapshot.leadership
        LifeReadinessAxisId.ETHICS -> snapshot.ethics
        LifeReadinessAxisId.COMMUNICATION -> snapshot.communication
    }

/**
 * Maps API soft counters to radar axes: wisdom→tech, money→business, social→leadership, balance→communication.
 * Ethics is a gentle blend of judgment + care (matches product framing when integrity isn’t a separate API pillar).
 */
fun lifeSkillCountersToLifeReadinessSnapshot(
    wisdom: Int,
    social: Int,
    money: Int,
    balance: Int,
): LifeReadinessSnapshot {
    val w = wisdom.coerceIn(0, 100)
    val s = social.coerceIn(0, 100)
    val m = money.coerceIn(0, 100)
    val b = balance.coerceIn(0, 100)
    val ethics = ((w + s + b) / 3).coerceIn(0, 100)
    return LifeReadinessSnapshot(
        tech = w,
        business = m,
        leadership = s,
        ethics = ethics,
        communication = b,
    )
}

fun readinessStatusTier(value: Int): Int {
    val v = value.coerceIn(0, 100)
    return when {
        v <= 24 -> 0
        v <= 49 -> 1
        v <= 69 -> 2
        v <= 84 -> 3
        v <= 94 -> 4
        else -> 5
    }
}

private const val BADGE_THRESHOLD = 70

data class WisdomBadge(
    val id: String,
    val axis: LifeReadinessAxisId,
)

fun wisdomBadgesUnlocked(snapshot: LifeReadinessSnapshot): List<WisdomBadge> {
    val out = ArrayList<WisdomBadge>(5)
    if (snapshot.tech >= BADGE_THRESHOLD) {
        out.add(WisdomBadge("scam-proof-senior", LifeReadinessAxisId.TECH))
    }
    if (snapshot.business >= BADGE_THRESHOLD) {
        out.add(WisdomBadge("kirana-king", LifeReadinessAxisId.BUSINESS))
    }
    if (snapshot.leadership >= BADGE_THRESHOLD) {
        out.add(WisdomBadge("team-captain", LifeReadinessAxisId.LEADERSHIP))
    }
    if (snapshot.ethics >= BADGE_THRESHOLD) {
        out.add(WisdomBadge("truth-seeker", LifeReadinessAxisId.ETHICS))
    }
    if (snapshot.communication >= BADGE_THRESHOLD) {
        out.add(WisdomBadge("clear-voice", LifeReadinessAxisId.COMMUNICATION))
    }
    return out
}

fun lowestReadinessAxis(snapshot: LifeReadinessSnapshot): LifeReadinessAxisId {
    var minAxis = LifeReadinessAxisId.TECH
    var minV = Int.MAX_VALUE
    for (def in LIFE_READINESS_AXIS_ORDER) {
        val v = valueForAxis(snapshot, def.id)
        if (v < minV) {
            minV = v
            minAxis = def.id
        }
    }
    return minAxis
}

/**
 * Library hub query value — must match [com.tamixa.ui.navigation.Screen.Library] hub arguments
 * and web library hub query values (`browse` / `fun` / `learn` / `simulator`).
 */
data class NextStoryRecommendation(
    val axis: LifeReadinessAxisId,
    val libraryHubParam: String,
)

private const val REC_HUB_SIMULATOR = "simulator"
private const val REC_HUB_LEARN = "learn"
private const val REC_HUB_BROWSE = "browse"

fun recommendationForAxis(axis: LifeReadinessAxisId): NextStoryRecommendation {
    return when (axis) {
        LifeReadinessAxisId.TECH ->
            NextStoryRecommendation(axis, REC_HUB_SIMULATOR)
        LifeReadinessAxisId.BUSINESS ->
            NextStoryRecommendation(axis, REC_HUB_LEARN)
        LifeReadinessAxisId.LEADERSHIP ->
            NextStoryRecommendation(axis, REC_HUB_BROWSE)
        LifeReadinessAxisId.ETHICS ->
            NextStoryRecommendation(axis, REC_HUB_SIMULATOR)
        LifeReadinessAxisId.COMMUNICATION ->
            NextStoryRecommendation(axis, REC_HUB_BROWSE)
    }
}

fun hasAnyPracticeSignal(wisdom: Int, social: Int, money: Int, balance: Int): Boolean =
    wisdom > 0 || social > 0 || money > 0 || balance > 0
