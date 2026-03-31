package com.tamixa.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.domain.SubscriptionInfo
import com.tamixa.domain.UsageInfo
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import com.tamixa.ui.theme.TamixaDialogDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    subscription: SubscriptionInfo?,
    usage: UsageInfo?,
    loading: Boolean,
    loadError: String? = null,
    canceling: Boolean,
    appliedReferral: com.tamixa.repository.ReferralCodeInfo?,
    referralError: String?,
    onLoadSubscription: () -> Unit,
    onCancelSubscription: () -> Unit,
    onApplyReferralCode: (String) -> Unit,
    onClearReferralCode: () -> Unit,
    onSubscribe: (referralCode: String?) -> Unit,
    onBack: () -> Unit
) {
    var showCancelConfirm by remember { mutableStateOf(false) }
    var referralCodeInput by remember { mutableStateOf("") }
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            shape = TamixaDialogDefaults.shape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    Strings.cancelAtPeriodEnd(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    Strings.cancelAtPeriodEndMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onCancelSubscription()
                    },
                    shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                ) { Text(Strings.continueWith()) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text(Strings.cancel()) }
            }
        )
    }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            com.tamixa.ui.components.TamixaScreenTopBar(
                title = Strings.subscription(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = TamixaDesignTokens.screenPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = TamixaDesignTokens.screenPaddingBottomWithNav)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Strings.chooseYourPlan(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    color = TamixaColors.cream
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (subscription?.isActive == true) Strings.subscriptionFullAccess() else Strings.subscribeToUnlock(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamixaColors.lavenderGlow.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(24.dp))

                if (loadError != null && !loading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                            Text(
                                text = loadError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = onLoadSubscription) {
                                Text(Strings.retry())
                            }
                        }
                    }
                    Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
                }

                val sub = subscription
                val isActive = sub?.isActive ?: false

                if (loading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActive) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge)
                                ) else Modifier
                            ),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isActive) Strings.activePlan() else Strings.noActivePlan(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!isActive) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "₹99",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "/${Strings.pricePerMonthInr()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            sub?.let {
                                if (it.planId != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "${Strings.plan()}: ${it.planId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                it.trialEnd?.takeIf { t -> t.isNotBlank() }?.let { end ->
                                    kotlin.runCatching {
                                        val endInstant = kotlinx.datetime.Instant.parse(end)
                                        val days = (endInstant - kotlinx.datetime.Clock.System.now()).inWholeDays
                                        if (days > 0) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                Strings.trialDaysLeft(days.toInt()),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }.getOrNull()
                                }
                                if (it.expiresAt != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${Strings.endsAt()}: ${it.expiresAt}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (it.cancelAtPeriodEnd) {
                                    Text(
                                        Strings.cancelsAtPeriodEnd(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    "${Strings.maxChildren()}: ${it.maxChildren}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = Strings.premiumBenefits(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TamixaColors.lavenderGlow.copy(alpha = 0.95f)
                )
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(TamixaDesignTokens.dialogRadius),
                    colors = TamixaCardColors.surface(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding),
                        verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.smallSpacing)
                    ) {
                        BenefitRow(icon = Icons.Outlined.Mic, text = Strings.benefitRecordYourVoice())
                        BenefitRow(icon = Icons.AutoMirrored.Outlined.MenuBook, text = Strings.benefitUnlimitedStories())
                        BenefitRow(icon = Icons.Outlined.Block, text = Strings.benefitAdFree())
                    }
                }

                usage?.let { u ->
                    Spacer(Modifier.height(TamixaDesignTokens.sectionSpacing))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                            Text(
                                Strings.usageThisMonth(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (u.storiesLimit != null && u.storiesLimit <= com.tamixa.util.TamixaConstants.FREE_PLAN_STORIES_LIMIT_THRESHOLD) Strings.freePlanLimit(u.storiesUsed, u.storiesLimit)
                                else "${Strings.storiesUsed()}: ${u.storiesUsed}${u.storiesLimit?.let { " / $it" } ?: " (unlimited)"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            u.storiesLimit?.let { limit ->
                                val remaining = (limit - u.storiesUsed).coerceAtLeast(0)
                                if (remaining <= 3 && remaining >= 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        Strings.storiesLeftThisMonth(remaining),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (remaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${Strings.voiceGenerations()}: ${u.voiceUsed} / ${u.voiceLimit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isActive) {
                    Spacer(Modifier.height(TamixaDesignTokens.cardSpacing))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding)) {
                            Text(
                                Strings.referralCodePlaceholder(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = referralCodeInput,
                                    onValueChange = { referralCodeInput = it.uppercase().take(32) },
                                    placeholder = { Text("AMAZ5", style = MaterialTheme.typography.bodyMedium) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                                )
                                Button(
                                    onClick = { onApplyReferralCode(referralCodeInput) },
                                    shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                                ) {
                                    Text(Strings.applyCode())
                                }
                            }
                            appliedReferral?.let { ref ->
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        Strings.referralDiscount(ref.offerPercent, ref.shopName),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = onClearReferralCode) {
                                        Text(Strings.cancel())
                                    }
                                }
                            }
                            referralError?.let { err ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                sub?.let {
                    if (!it.cancelAtPeriodEnd && it.planId != null && it.planId != com.tamixa.util.TamixaConstants.PLAN_FREE) {
                        OutlinedButton(
                            onClick = { showCancelConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius),
                            enabled = !canceling,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(if (canceling) Strings.canceling() else Strings.cancelAtPeriodEnd())
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                TamixaPrimaryButton(
                    onClick = { onSubscribe(appliedReferral?.shortcode) },
                    text = if (isActive) Strings.manage() else "${Strings.subscribe()} — ₹99/${Strings.pricePerMonthInr()}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(TamixaDesignTokens.smallSpacing))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = Strings.included(),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
    }
}
