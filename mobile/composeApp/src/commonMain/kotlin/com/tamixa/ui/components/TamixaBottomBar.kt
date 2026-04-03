package com.tamixa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaChrome
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.luminance

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

private val FloatingBarShape = RoundedCornerShape(34.dp)

/**
 * Floating “island” nav — frosted pill dock over the starfield (modern app chrome). Same four
 * destinations as before; selected tab gets a soft terracotta pill behind the icon.
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val muted =
        if (isDark) TamixaColors.cream.copy(alpha = 0.58f)
        else TamixaColors.appTextSecondary.copy(alpha = 0.88f)
    val barFill = TamixaChrome.bottomBarContainerColor()
    val barBorder = TamixaChrome.chromeOutlineColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = FloatingBarShape,
                        ambientColor = Color.Black.copy(alpha = 0.38f),
                        spotColor = accent.copy(alpha = 0.14f),
                    )
                    .clip(FloatingBarShape),
                shape = FloatingBarShape,
                color = barFill,
                border = BorderStroke(1.dp, barBorder),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TamixaNavItem(
                        modifier = Modifier.weight(1f),
                        selected = selectedTab == TamixaTab.Home,
                        onClick = onHome,
                        accent = accent,
                        muted = muted,
                        icon = { sel ->
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = Strings.home(),
                                tint = if (sel) accent else muted,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = Strings.home()
                    )
                    TamixaNavItem(
                        modifier = Modifier.weight(1f),
                        selected = selectedTab == TamixaTab.Library,
                        onClick = onLibrary,
                        accent = accent,
                        muted = muted,
                        icon = { sel ->
                            Icon(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = Strings.library(),
                                tint = if (sel) accent else muted,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = Strings.library()
                    )
                    TamixaNavItem(
                        modifier = Modifier.weight(1f),
                        selected = selectedTab == TamixaTab.FunAndLearn,
                        onClick = onFunAndLearn,
                        accent = accent,
                        muted = muted,
                        icon = { sel ->
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = Strings.funAndLearn(),
                                tint = if (sel) accent else muted,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = Strings.funAndLearn()
                    )
                    TamixaNavItem(
                        modifier = Modifier.weight(1f),
                        selected = selectedTab == TamixaTab.Profile ||
                            selectedTab == TamixaTab.MyVoiceAndAvatar ||
                            selectedTab == TamixaTab.Settings,
                        onClick = onProfile,
                        accent = accent,
                        muted = muted,
                        icon = { sel ->
                            Icon(
                                Icons.Default.Person,
                                contentDescription = Strings.profile(),
                                tint = if (sel) accent else muted,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = Strings.profile()
                    )
                }
            }
        }
    }
}

@Composable
private fun TamixaNavItem(
    modifier: Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    muted: Color,
    icon: @Composable (Boolean) -> Unit,
    label: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(accent.copy(alpha = 0.24f))
                )
            }
            icon(selected)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else muted,
            maxLines = 1,
        )
    }
}
