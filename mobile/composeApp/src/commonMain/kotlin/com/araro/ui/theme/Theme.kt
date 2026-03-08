package com.araro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Araro: Premium dark theme ----
// Near-black background, gold accents, immersive storytelling look

// Araro dark palette
private val AraroDarkBg = Color(0xFF0D0D0D)
private val AraroSurface = Color(0xFF141414)
private val AraroSurfaceVariant = Color(0xFF1F1F1F)
private val AraroBorder = Color(0xFF2A2A2A)
private val SoftGold = Color(0xFFF6C453)
private val Cream = Color(0xFFF5F5F5)
private val GrayText = Color(0xFFB3B3B3)

// Legacy (kept for compatibility)
private val MidnightBlue = Color(0xFF1E1F3F)
private val DeepIndigo = Color(0xFF2D2F6E)
private val LavenderGlow = Color(0xFFE8E0F0)
private val SoftBlue = Color(0xFF5B9BD5)
private val ErrorRed = Color(0xFFE57373)
private val OnError = Color(0xFF2D1520)

// Theme mappings (primary = Soft Gold, background = gradient)
private val OnSoftGold = Color(0xFF2D2310)
private val NightSkyBg = MidnightBlue
private val NightSkySurface = DeepIndigo
private val NightSkySurfaceVariant = Color(0xFF3D3F7E)
private val YellowOrangePrimary = SoftGold
private val YellowOrangeSecondary = Color(0xFFE5B03D)
private val OnYellowOrange = OnSoftGold
private val OnNightSky = Cream
private val OnNightSkyVariant = LavenderGlow.copy(alpha = 0.9f)
private val StarAccent = LavenderGlow
private val CloudLight = LavenderGlow

// Light theme (fallback)
private val MaroonPrimary = Color(0xFF6B2D3C)
private val MaroonPrimaryVariant = Color(0xFF8B3D4D)
private val GoldAccent = Color(0xFFC9A227)
private val GoldAccentLight = Color(0xFFE8D48B)
private val CreamBackground = Color(0xFFFDF8F3)
private val CreamSurface = Color(0xFFFFFBF7)
private val WarmSurfaceVariant = Color(0xFFF5EDE4)
private val OnMaroon = Color(0xFFFFFFFF)
private val OnGold = Color(0xFF2D2310)
private val OnCream = Color(0xFF2C2520)
private val OnSurfaceVariantLight = Color(0xFF5C534A)
private val OutlineLight = Color(0xFFC4B5A5)

// Legacy dark theme
private val DarkMaroonPrimary = Color(0xFFE8A0B0)
private val DarkMaroonPrimaryVariant = Color(0xFF8B3D4D)
private val DarkGoldAccent = Color(0xFFE8D48B)
private val DarkBackground = Color(0xFF1A1512)
private val DarkSurface = Color(0xFF241E1A)
private val DarkSurfaceVariant = Color(0xFF3D3530)
private val DarkOnPrimary = Color(0xFF3D1520)
private val DarkOnBackground = Color(0xFFF5EDE4)
private val DarkOnSurface = Color(0xFFF5EDE4)
private val DarkOnSurfaceVariant = Color(0xFFD4C9BC)
private val DarkError = Color(0xFFF2B8B5)
private val DarkOnError = Color(0xFF601410)
private val OutlineDark = Color(0xFF6B6156)

private val NightSkyScheme = darkColorScheme(
    primary = SoftGold,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = AraroSurfaceVariant,
    onPrimaryContainer = Cream,
    secondary = StarAccent,
    onSecondary = OnNightSky,
    secondaryContainer = AraroSurfaceVariant,
    onSecondaryContainer = OnNightSkyVariant,
    tertiary = CloudLight,
    onTertiary = OnNightSky,
    tertiaryContainer = AraroSurfaceVariant,
    onTertiaryContainer = OnNightSkyVariant,
    error = ErrorRed,
    onError = OnError,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = ErrorRed,
    background = AraroDarkBg,
    onBackground = Cream,
    surface = AraroSurface,
    onSurface = Cream,
    surfaceVariant = AraroSurfaceVariant,
    onSurfaceVariant = OnNightSkyVariant,
    outline = OnNightSkyVariant.copy(alpha = 0.5f),
    outlineVariant = AraroSurfaceVariant
)

private val LightScheme = lightColorScheme(
    primary = MaroonPrimary,
    onPrimary = OnMaroon,
    primaryContainer = MaroonPrimaryVariant,
    onPrimaryContainer = OnMaroon,
    secondary = GoldAccent,
    onSecondary = OnGold,
    secondaryContainer = GoldAccentLight,
    onSecondaryContainer = OnGold,
    tertiary = GoldAccentLight,
    onTertiary = OnGold,
    tertiaryContainer = WarmSurfaceVariant,
    onTertiaryContainer = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnError,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = CreamBackground,
    onBackground = OnCream,
    surface = CreamSurface,
    onSurface = OnCream,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE7DED4)
)

private val DarkScheme = darkColorScheme(
    primary = DarkMaroonPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkMaroonPrimaryVariant,
    onPrimaryContainer = DarkMaroonPrimary,
    secondary = DarkGoldAccent,
    onSecondary = Color(0xFF3D3520),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurfaceVariant,
    tertiary = DarkGoldAccent,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = DarkError,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = OutlineDark,
    outlineVariant = DarkSurfaceVariant
)

// Design spec: 24dp corners for large elements, 16dp for inputs
private val AraroShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

// Design spec: cream text on dark, dark text on light surfaces
private val AraroTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun AraroTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NightSkyScheme else LightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AraroShapes,
        typography = AraroTypography,
        content = content
    )
}

// HD-quality: named colors for gradients, glows, premium components
object AraroColors {
    val goldAccent: Color get() = SoftGold
    val goldAccentLight: Color get() = YellowOrangeSecondary
    val maroonPrimary: Color get() = SoftGold
    val maroonPrimaryVariant: Color get() = YellowOrangeSecondary
    val creamBackground: Color get() = AraroDarkBg
    val creamSurface: Color get() = AraroSurface
    val warmSurfaceVariant: Color get() = AraroSurfaceVariant
    val yellowOrangePrimary: Color get() = YellowOrangePrimary
    val yellowOrangeSecondary: Color get() = YellowOrangeSecondary
    val nightSkyBg: Color get() = AraroDarkBg
    val nightSkySurface: Color get() = AraroSurface
    /** White rounded input surface for night sky screens (matches screenshot) */
    val inputSurface: Color get() = Color(0xFFFFFFFF)
    /** Text color on white inputs (dark for contrast) */
    val onInputSurface: Color get() = Color(0xFF2C2520)
    /** For filter chips background */
    val nightSkySurfaceVariant: Color get() = AraroSurfaceVariant
    /** Cream text on dark backgrounds (design spec) */
    val cream: Color get() = Cream
    /** Lavender for secondary text/icons */
    val lavenderGlow: Color get() = LavenderGlow
    /** Secondary text for metadata and labels */
    val grayText: Color get() = GrayText
}

/** Design spec tokens: card radii, spacing, elevation */
object AraroDesignTokens {
    val cardElevation = 2.dp
    val cardElevationHover = 6.dp
    val dialogRadius = 24.dp
    val buttonRadius = 24.dp
    val inputRadius = 16.dp
    /** Card corner radius for compact layouts */
    val cardRadius = 6.dp
    val screenPadding = 24.dp
    val headerPaddingHorizontal = 20.dp
    val headerPaddingVertical = 18.dp
    val contentPaddingHorizontal = 16.dp
    val sectionSpacing = 24.dp
    val cardSpacing = 12.dp
}

/** Shared styling for Araro dialogs: shape and surface for kid + parent appeal. */
object AraroDialogDefaults {
    val shape get() = androidx.compose.foundation.shape.RoundedCornerShape(AraroDesignTokens.dialogRadius)
}

object AraroGradients {
    val lightBackground = listOf(AraroDarkBg, AraroSurface)
    val lightBackgroundWithAccent = listOf(AraroDarkBg, AraroSurface)
    /** Dark theme gradient */
    val nightSkyBackground = listOf(
        Color(0xFF0D0D0D),
        Color(0xFF141414),
        Color(0xFF1A1A1A)
    )
    /** Image overlay gradient (transparent → dark at bottom) */
    val imageOverlay = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.3f),
        Color.Black.copy(alpha = 0.85f)
    )
    /** Soft gold primary CTAs */
    val yellowOrangeButton = listOf(SoftGold, YellowOrangeSecondary)

    fun yellowOrangeButtonBrush() = Brush.verticalGradient(yellowOrangeButton)
}

/** Design spec colors for components */
object AraroDesignColors {
    val midnightBlue = Color(0xFF1E1F3F)
    val deepIndigo = Color(0xFF2D2F6E)
    val softGold = Color(0xFFF6C453)
    val cream = Color(0xFFFDF6E3)
    val lavenderGlow = Color(0xFFE8E0F0)
    val softBlue = Color(0xFF5B9BD5)
}
