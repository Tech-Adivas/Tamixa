package com.tamixa.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.tamixa_app_icon
import org.jetbrains.compose.resources.painterResource

/**
 * App launcher icon as in-app logo (login, dashboard, language selection, settings).
 */
@Composable
fun TamixaFullLogo(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    Image(
        painter = painterResource(Res.drawable.tamixa_app_icon),
        contentDescription = "Tamixa",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
