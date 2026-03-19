package com.tamixa.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors

enum class TamixaTab {
    Home,
    Library,
    FunAndLearn,
    Profile,
    /** Voice/Avatar hub; show Profile tab as selected. */
    MyVoiceAndAvatar,
    /** Settings; show Profile tab as selected. */
    Settings
}

/**
 * Bottom navigation bar: Home | Library | Fun & Learn | Profile.
 * Matches top bar: solid dark background (#0F0E17) with cream labels and gold accent for selected.
 */
@Composable
fun TamixaBottomBar(
    selectedTab: TamixaTab,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onFunAndLearn: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = Color(0xFF1A1812)  // Storybook Dusk warm charcoal
    val unselectedColor = TamixaColors.cream.copy(alpha = 0.85f)
    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = TamixaColors.cream,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == TamixaTab.Home,
            onClick = onHome,
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = Strings.home(),
                    tint = if (selectedTab == TamixaTab.Home) TamixaColors.goldAccent else unselectedColor
                )
            },
            label = {
                Text(
                    Strings.home(),
                    color = if (selectedTab == TamixaTab.Home) TamixaColors.goldAccent else unselectedColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
        NavigationBarItem(
            selected = selectedTab == TamixaTab.Library,
            onClick = onLibrary,
            icon = {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = Strings.library(),
                    tint = if (selectedTab == TamixaTab.Library) TamixaColors.goldAccent else unselectedColor
                )
            },
            label = {
                Text(
                    Strings.library(),
                    color = if (selectedTab == TamixaTab.Library) TamixaColors.goldAccent else unselectedColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
        NavigationBarItem(
            selected = selectedTab == TamixaTab.FunAndLearn,
            onClick = onFunAndLearn,
            icon = {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = Strings.funAndLearn(),
                    tint = if (selectedTab == TamixaTab.FunAndLearn) TamixaColors.goldAccent else unselectedColor
                )
            },
            label = {
                Text(
                    Strings.funAndLearn(),
                    color = if (selectedTab == TamixaTab.FunAndLearn) TamixaColors.goldAccent else unselectedColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
        NavigationBarItem(
            selected = selectedTab == TamixaTab.Profile || selectedTab == TamixaTab.MyVoiceAndAvatar || selectedTab == TamixaTab.Settings,
            onClick = onProfile,
            icon = {
                val profileSelected = selectedTab == TamixaTab.Profile || selectedTab == TamixaTab.MyVoiceAndAvatar || selectedTab == TamixaTab.Settings
                Icon(
                    Icons.Default.Person,
                    contentDescription = Strings.profile(),
                    tint = if (profileSelected) TamixaColors.goldAccent else unselectedColor
                )
            },
            label = {
                val profileSelected = selectedTab == TamixaTab.Profile || selectedTab == TamixaTab.MyVoiceAndAvatar || selectedTab == TamixaTab.Settings
                Text(
                    Strings.profile(),
                    color = if (profileSelected) TamixaColors.goldAccent else unselectedColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
    }
}
