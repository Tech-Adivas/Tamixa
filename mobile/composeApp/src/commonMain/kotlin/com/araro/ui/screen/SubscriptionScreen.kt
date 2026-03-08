package com.araro.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import com.araro.domain.SubscriptionInfo
import com.araro.domain.UsageInfo
import com.araro.ui.components.AraroEmojiDisplay
import com.araro.ui.strings.Strings
import com.araro.ui.components.StarryNightBackground
import com.araro.ui.theme.AraroColors
import com.araro.ui.theme.AraroDesignTokens
import com.araro.ui.theme.AraroDialogDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    subscription: SubscriptionInfo?,
    usage: UsageInfo?,
    loading: Boolean,
    canceling: Boolean,
    onLoadSubscription: () -> Unit,
    onCancelSubscription: () -> Unit,
    onManageSubscription: () -> Unit,
    onBack: () -> Unit
) {
    var showCancelConfirm by remember { mutableStateOf(false) }
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            shape = AraroDialogDefaults.shape,
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
                    shape = RoundedCornerShape(AraroDesignTokens.buttonRadius)
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
            com.araro.ui.components.AraroScreenTopBar(
                title = Strings.subscription(),
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            StarryNightBackground(showClouds = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val sub = subscription
            val isActive = sub?.isActive ?: false

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AraroEmojiDisplay(
                    emoji = if (isActive) "✨📋" else "📋💫",
                    fontSize = 48.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AraroDesignTokens.dialogRadius),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) AraroColors.goldAccentLight.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        if (isActive) Strings.activePlan() else Strings.noActivePlan(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive) AraroColors.maroonPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    sub?.let {
                        if (it.planId != null) Text("${Strings.plan()}: ${it.planId}", style = MaterialTheme.typography.bodyMedium)
                        it.trialEnd?.takeIf { t -> t.isNotBlank() }?.let { end ->
                            kotlin.runCatching {
                                val endInstant = kotlinx.datetime.Instant.parse(end)
                                val days = (endInstant - kotlinx.datetime.Clock.System.now()).inWholeDays
                                if (days > 0) Text(Strings.trialDaysLeft(days.toInt()), style = MaterialTheme.typography.bodyMedium, color = AraroColors.maroonPrimary)
                            }.getOrNull()
                        }
                        if (it.expiresAt != null) Text("${Strings.endsAt()}: ${it.expiresAt}", style = MaterialTheme.typography.bodySmall)
                        if (it.cancelAtPeriodEnd) Text(Strings.cancelsAtPeriodEnd(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text("${Strings.maxChildren()}: ${it.maxChildren}", style = MaterialTheme.typography.bodySmall)
                    } ?: Text(
                        if (isActive) Strings.subscriptionFullAccess() else Strings.subscribeToUnlock(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            usage?.let { u ->
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AraroDesignTokens.dialogRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(Strings.usageThisMonth(), style = MaterialTheme.typography.titleMedium, color = AraroColors.maroonPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (u.storiesLimit != null && u.storiesLimit <= com.araro.util.AraroConstants.FREE_PLAN_STORIES_LIMIT_THRESHOLD) Strings.freePlanLimit(u.storiesUsed, u.storiesLimit)
                            else "${Strings.storiesUsed()}: ${u.storiesUsed}${u.storiesLimit?.let { " / $it" } ?: " (unlimited)"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("${Strings.voiceGenerations()}: ${u.voiceUsed} / ${u.voiceLimit}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            sub?.let {
                if (!it.cancelAtPeriodEnd && it.planId != null && it.planId != com.araro.util.AraroConstants.PLAN_FREE) {
                    OutlinedButton(
                        onClick = { showCancelConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AraroDesignTokens.buttonRadius),
                        enabled = !canceling
                    ) {
                        Text(if (canceling) Strings.canceling() else Strings.cancelAtPeriodEnd())
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            com.araro.ui.components.AraroPrimaryButton(
                onClick = onManageSubscription,
                text = if (isActive) Strings.manage() else Strings.subscribe(),
                modifier = Modifier.fillMaxWidth()
            )
            }
        }
    }
}
