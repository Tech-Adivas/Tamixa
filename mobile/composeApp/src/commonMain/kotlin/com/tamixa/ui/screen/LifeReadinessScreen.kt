package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tamixa.domain.LIFE_READINESS_AXIS_ORDER
import com.tamixa.domain.LifeReadinessAxisId
import com.tamixa.domain.LifeReadinessSnapshot
import com.tamixa.domain.hasAnyPracticeSignal
import com.tamixa.domain.lifeSkillCountersToLifeReadinessSnapshot
import com.tamixa.domain.lowestReadinessAxis
import com.tamixa.domain.readinessStatusTier
import com.tamixa.domain.recommendationForAxis
import com.tamixa.domain.valueForAxis
import com.tamixa.domain.wisdomBadgesUnlocked
import com.tamixa.network.LifeSkillCountersResponseDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.LifeReadinessRadarChart
import com.tamixa.ui.components.TamixaBottomBar
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.components.TamixaTab
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.navigation.Screen
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaContentColors
import com.tamixa.ui.theme.TamixaDesignTokens

@Composable
private fun axisLabel(id: LifeReadinessAxisId): String =
    when (id) {
        LifeReadinessAxisId.TECH -> Strings.lifeReadinessAxisTech()
        LifeReadinessAxisId.BUSINESS -> Strings.lifeReadinessAxisBusiness()
        LifeReadinessAxisId.LEADERSHIP -> Strings.lifeReadinessAxisLeadership()
        LifeReadinessAxisId.ETHICS -> Strings.lifeReadinessAxisEthics()
        LifeReadinessAxisId.COMMUNICATION -> Strings.lifeReadinessAxisCommunication()
    }

@Composable
private fun axisNote(id: LifeReadinessAxisId): String =
    when (id) {
        LifeReadinessAxisId.TECH -> Strings.lifeReadinessAxisNoteTech()
        LifeReadinessAxisId.BUSINESS -> Strings.lifeReadinessAxisNoteBusiness()
        LifeReadinessAxisId.LEADERSHIP -> Strings.lifeReadinessAxisNoteLeadership()
        LifeReadinessAxisId.ETHICS -> Strings.lifeReadinessAxisNoteEthics()
        LifeReadinessAxisId.COMMUNICATION -> Strings.lifeReadinessAxisNoteCommunication()
    }

@Composable
fun LifeReadinessScreen(
    counters: LifeSkillCountersResponseDto?,
    countersLoading: Boolean,
    lifeSkillChildOptions: List<Pair<Long, String>> = emptyList(),
    selectedLifeSkillChildId: Long? = null,
    onLifeSkillChildChange: (Long) -> Unit = {},
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToShortContent: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLibraryHub: (hubParam: String) -> Unit,
) {
    val snapshot: LifeReadinessSnapshot? = counters?.let {
        lifeSkillCountersToLifeReadinessSnapshot(it.wisdom, it.social, it.money, it.balance)
    }
    val hasSignal = counters != null &&
        hasAnyPracticeSignal(counters.wisdom, counters.social, counters.money, counters.balance)

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = Strings.lifeReadinessScreenTitle(),
                onBack = onBack,
                useTransparentBackground = true,
            )
        },
        bottomBar = {
            TamixaBottomBar(
                selectedTab = TamixaTab.Home,
                onHome = onNavigateToHome,
                onLibrary = onNavigateToLibrary,
                onFunAndLearn = onNavigateToShortContent,
                onProfile = onNavigateToProfile,
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppScreenBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = TamixaDesignTokens.screenPadding,
                        top = TamixaDesignTokens.screenPadding,
                        end = TamixaDesignTokens.screenPadding,
                        bottom = 0.dp,
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing),
            ) {
                Text(
                    text = Strings.lifeReadinessHeroEyebrow(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TamixaColors.goldAccent.copy(alpha = 0.9f),
                )
                Text(
                    text = Strings.lifeReadinessSubtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamixaColors.cream.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(4.dp))

                if (lifeSkillChildOptions.size > 1 && selectedLifeSkillChildId != null) {
                    var childMenuExpanded by remember { mutableStateOf(false) }
                    val selectedLabel = lifeSkillChildOptions.find { it.first == selectedLifeSkillChildId }?.second
                        ?: Strings.lifeSkillPracticeUnnamedChild()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { childMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${Strings.lifeSkillPracticeChooseChild()}: $selectedLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TamixaContentColors.cardPrimary(),
                            )
                        }
                        DropdownMenu(
                            expanded = childMenuExpanded,
                            onDismissRequest = { childMenuExpanded = false },
                        ) {
                            lifeSkillChildOptions.forEach { (cid, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        onLifeSkillChildChange(cid)
                                        childMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                when {
                    countersLoading && counters == null -> {
                        Text(
                            text = Strings.lifeReadinessLoading(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaColors.cream.copy(alpha = 0.75f),
                        )
                    }
                    snapshot == null -> {
                        Text(
                            text = Strings.lifeReadinessEmptyBody(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaColors.cream.copy(alpha = 0.8f),
                        )
                        TamixaPrimaryButton(
                            onClick = { onNavigateToLibraryHub(Screen.Library.HUB_SIMULATOR) },
                            text = Strings.lifeReadinessOpenPracticeHub(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        if (!hasSignal) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TamixaCardColors.surface(),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        text = Strings.lifeReadinessEmptyTitle(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TamixaColors.cream,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = Strings.lifeReadinessEmptyBody(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TamixaColors.cream.copy(alpha = 0.72f),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    TamixaPrimaryButton(
                                        onClick = {
                                            onNavigateToLibraryHub(Screen.Library.HUB_SIMULATOR)
                                        },
                                        text = Strings.lifeReadinessOpenPracticeHub(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            text = Strings.lifeReadinessSectionHowFeels(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TamixaColors.cream,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            LifeReadinessRadarChart(snapshot = snapshot, modifier = Modifier)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                for (def in LIFE_READINESS_AXIS_ORDER) {
                                    val v = valueForAxis(snapshot, def.id)
                                    val tier = readinessStatusTier(v)
                                    Column {
                                        Row {
                                            Text(
                                                text = axisLabel(def.id),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TamixaColors.cream,
                                            )
                                            Text(
                                                text = " · ${Strings.lifeReadinessStatusLabel(tier)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TamixaColors.cream.copy(alpha = 0.65f),
                                            )
                                        }
                                        Text(
                                            text = axisNote(def.id),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TamixaColors.cream.copy(alpha = 0.55f),
                                        )
                                    }
                                }
                            }
                        }

                        val badges = wisdomBadgesUnlocked(snapshot)
                        if (badges.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = Strings.lifeReadinessSectionBadges(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TamixaColors.cream,
                            )
                            for (b in badges) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = TamixaCardColors.surface(),
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            text = Strings.lifeReadinessBadgeTitle(b.id),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TamixaColors.cream,
                                        )
                                        Text(
                                            text = Strings.lifeReadinessBadgeLine(b.id),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TamixaColors.cream.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Strings.lifeReadinessSectionNext(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TamixaColors.cream,
                        )
                        val focus = lowestReadinessAxis(snapshot)
                        val rec = recommendationForAxis(focus)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TamixaCardColors.surface(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    text = Strings.lifeReadinessRecHeadline(focus),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TamixaColors.cream,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = Strings.lifeReadinessRecMessage(focus),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TamixaColors.cream.copy(alpha = 0.72f),
                                )
                                Spacer(Modifier.height(10.dp))
                                TamixaPrimaryButton(
                                    onClick = { onNavigateToLibraryHub(rec.libraryHubParam) },
                                    text = Strings.lifeReadinessPickStory(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
