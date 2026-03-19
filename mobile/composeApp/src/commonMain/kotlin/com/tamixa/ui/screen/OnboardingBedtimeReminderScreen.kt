package com.tamixa.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

@Composable
fun OnboardingBedtimeReminderScreen(
    onContinue: (reminderEnabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var reminderEnabled by remember { mutableStateOf(true) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onboardingSwipeNavigation(onSwipeToNext, onSwipeToPrevious)
    ) {
        AppScreenBackground(showClouds = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TamixaDesignTokens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = Strings.remindMeAtBedtime(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = OnboardingCardColors.onboardingHeadline,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = Strings.bedtimeReminderSubline(),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = OnboardingCardColors.onboardingSubline,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.enableReminder(),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnboardingCardColors.onboardingHeadline
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TamixaColors.cream,
                            checkedTrackColor = TamixaColors.purple,
                            uncheckedThumbColor = TamixaColors.lavenderGlow.copy(alpha = 0.7f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            TamixaPrimaryButton(
                onClick = {
                    val (hour, minute) = 20 to 0
                    onContinue(reminderEnabled, hour, minute)
                },
                text = Strings.continueLabel(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
