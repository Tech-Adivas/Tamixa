package com.tamixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
 * Full-width bottom dock: **Storybook Dusk night** background ([TamixaColors.nightSkyBg]) aligned with
 * [AppScreenBackground], slim accent line, terracotta selection + cream labels.
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
    val accent = TamixaColors.goldAccent
    val muted = TamixaColors.cream.copy(alpha = 0.66f)
    val contentColor = TamixaColors.cream
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = accent,
        selectedTextColor = accent,
        indicatorColor = accent.copy(alpha = 0.22f),
        unselectedIconColor = muted,
        unselectedTextColor = muted
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.38f),
                            TamixaColors.deepTeal.copy(alpha = 0.34f),
                            accent.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                containerColor = TamixaColors.nightSkyBg,
                contentColor = contentColor,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == TamixaTab.Home,
                    onClick = onHome,
                    icon = {
                        Icon(Icons.Filled.Home, contentDescription = Strings.home())
                    },
                    label = { Text(Strings.home(), style = MaterialTheme.typography.labelMedium) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == TamixaTab.Library,
                    onClick = onLibrary,
                    icon = {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = Strings.library())
                    },
                    label = { Text(Strings.library(), style = MaterialTheme.typography.labelMedium) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == TamixaTab.FunAndLearn,
                    onClick = onFunAndLearn,
                    icon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = Strings.funAndLearn())
                    },
                    label = { Text(Strings.funAndLearn(), style = MaterialTheme.typography.labelMedium) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == TamixaTab.Profile ||
                        selectedTab == TamixaTab.MyVoiceAndAvatar ||
                        selectedTab == TamixaTab.Settings,
                    onClick = onProfile,
                    icon = {
                        Icon(Icons.Default.Person, contentDescription = Strings.profile())
                    },
                    label = { Text(Strings.profile(), style = MaterialTheme.typography.labelMedium) },
                    colors = itemColors
                )
            }
        }
    }
}
