package com.tamixa.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Consistent content area for app screens: same padding and optional scroll.
 * Use so every screen (kids to elders) has a professional, HD feel and clear hierarchy.
 *
 * @param withBottomNav When true, adds extra bottom padding so content isn't hidden under the bottom bar.
 * @param scroll When true, content scrolls; when false, content is static (e.g. dashboard with FAB).
 * @param content Main screen content; receives padding from [TamixaDesignTokens].
 */
@Composable
fun AppScreenLayout(
    modifier: Modifier = Modifier,
    withBottomNav: Boolean = true,
    scroll: Boolean = true,
    content: @Composable () -> Unit
) {
    val bottomPadding = if (withBottomNav) TamixaDesignTokens.screenPaddingBottomWithNav else 32.dp
    val contentModifier = modifier
        .fillMaxSize()
        .padding(
            start = TamixaDesignTokens.screenPadding,
            top = TamixaDesignTokens.screenPadding,
            end = TamixaDesignTokens.screenPadding,
            bottom = bottomPadding
        )
    if (scroll) {
        Column(
            modifier = contentModifier.verticalScroll(rememberScrollState())
        ) {
            content()
        }
    } else {
        Box(modifier = contentModifier) {
            content()
        }
    }
}
