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
import androidx.compose.ui.text.style.TextOverflow
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.TamixaChrome
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.luminance

/**
 * Common app bar for all screens.
 * - [useTransparentBackground] = true: glassy bar over starfield; **cream** chrome on dark theme
 *   so titles read like a modern streaming / wellness app (not default M3 onSurface).
 * - [useTransparentBackground] = false: solid surface + theme onSurface.
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
    val onDarkStarfield = colorScheme.background.luminance() < 0.5f
    val titleColor =
        if (useTransparentBackground && onDarkStarfield) TamixaColors.cream
        else colorScheme.onSurface
    val navigationIconColor = titleColor

    if (useTransparentBackground) {
        val darkBarBg = TamixaChrome.barContainerColor()
        TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
