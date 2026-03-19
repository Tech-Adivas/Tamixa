package com.tamixa.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.tamixa_logo_full
import org.jetbrains.compose.resources.painterResource

/**
 * Full Tamixa logo (logo 5) for login, dashboard, language selection, settings.
 */
@Composable
fun TamixaFullLogo(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    Image(
        painter = painterResource(Res.drawable.tamixa_logo_full),
        contentDescription = "Tamixa",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
