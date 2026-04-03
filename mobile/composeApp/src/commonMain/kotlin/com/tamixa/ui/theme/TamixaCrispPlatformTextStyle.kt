package com.tamixa.ui.theme

import androidx.compose.ui.text.PlatformTextStyle

/** Android: no extra font padding; iOS/other: platform default (no `includeFontPadding` API). */
internal expect val TamixaCrispPlatformTextStyle: PlatformTextStyle
