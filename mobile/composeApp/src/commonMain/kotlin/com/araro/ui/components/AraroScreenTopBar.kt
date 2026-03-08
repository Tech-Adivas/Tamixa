package com.araro.ui.components

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
import com.araro.ui.strings.Strings
import com.araro.ui.theme.AraroColors

/**
 * Reusable app bar for secondary screens with consistent back/close navigation and title.
 * Use [useTransparentBackground] for screens with StarryNightBackground (e.g. Subscription, Favorites).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AraroScreenTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    useTransparentBackground: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (useTransparentBackground) Color.Transparent else colorScheme.surface
    val titleColor = if (useTransparentBackground) AraroColors.cream else colorScheme.onSurface
    val navigationIconColor = titleColor

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
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
