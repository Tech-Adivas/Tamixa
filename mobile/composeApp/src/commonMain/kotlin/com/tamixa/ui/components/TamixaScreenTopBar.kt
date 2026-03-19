package com.tamixa.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaColors

/**
 * Common app bar for all screens.
 * - [useTransparentBackground] = true: cream title/icons on solid dark bar (#0F0E17)
 * - [useTransparentBackground] = false: uses surface container with theme colors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TamixaScreenTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    useTransparentBackground: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (useTransparentBackground) Color.Transparent else colorScheme.surface
    val titleColor = if (useTransparentBackground) TamixaColors.cream else colorScheme.onSurface
    val navigationIconColor = titleColor

    if (useTransparentBackground) {
        // Solid dark background so cream text is always visible (avoids light blue showing through)
        val darkBarBg = Color(0xFF1A1812)  // Storybook Dusk warm charcoal
        TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = titleColor
                    )
                },
                navigationIcon = {
                    when {
                        onClose != null -> {
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = Strings.close(),
                                    tint = navigationIconColor
                                )
                            }
                        }
                        onBack != null -> {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = Strings.back(),
                                    tint = navigationIconColor
                                )
                            }
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBarBg,
                    scrolledContainerColor = darkBarBg,
                    titleContentColor = titleColor,
                    navigationIconContentColor = navigationIconColor,
                    actionIconContentColor = navigationIconColor
                ),
            )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = titleColor
                )
            },
            navigationIcon = {
                when {
                    onClose != null -> {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = Strings.close(),
                                tint = navigationIconColor
                            )
                        }
                    }
                    onBack != null -> {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Strings.back(),
                                tint = navigationIconColor
                            )
                        }
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = titleColor,
                navigationIconContentColor = navigationIconColor,
                actionIconContentColor = navigationIconColor
            ),
        )
    }
}
