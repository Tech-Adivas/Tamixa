@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.tamixa.ui.theme

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.tamixaUiSansFont
import org.jetbrains.compose.resources.Font

// ---- Tamixa Design System: Listen • Learn • Shine ----
// UNIQUE "Storybook Dusk" theme — differentiates from Duolingo (bright white), Spotify (blue),
// Moonlit (cold dark), generic purple gradients. Earthy terracotta + deep teal + warm cream.
// Research: audiobook apps → warmer palettes; kids apps → avoid cold; bedtime → warm undertones.

// Storybook Dusk palette (primary — distinctive, storytelling)
private val Terracotta = Color(0xFFC4625A)      // Warm accent: clay, storytelling, inviting
private val DeepTeal = Color(0xFF2D5A5A)       // Primary: calm, trust, bedtime
private val TealMuted = Color(0xFF3D6B6B)      // Lighter teal for surfaces
/** Shared mint end-stop for edu-story / Learn-lane gradients and accents. */
private val EduStoryMint = Color(0xFF5CBCA8)
private val WarmSand = Color(0xFFE8DCC8)        // Cream on dark: warm, not cold
private val WarmCharcoal = Color(0xFF141210)   // Deeper OLED-friendly base (modern depth)
private val WarmSurface = Color(0xFF2C2822)    // Lifted card tier — clearer layering vs bg
private val WarmSurfaceVariant = Color(0xFF3A342E)

// Legacy / fallback (kept for gradients, onboarding)
private val FigmaWarmOrange = Color(0xFFFF8A3D)
private val FigmaSkyBlue = Color(0xFF6EC3FF)
private val FigmaSoftPurple = Color(0xFF9D8CFF)
private val FigmaMintGreen = Color(0xFF7ED9C4)
private val FigmaLightBg = Color(0xFFF6F8FB)
private val TamixaBlue = Color(0xFF3B82F6)
private val TamixaPurple = Color(0xFF7C3AED)
private val TamixaYellow = Color(0xFFFFD84D)
private val WarmAmber = Color(0xFFEAB308)
private val TamixaOrange = Color(0xFFFF8A00)
private val TamixaPink = Color(0xFFFF4D8D)
private val TamixaIndigo = Color(0xFF4F46E5)

// Dark theme: warm charcoal base (Storybook Dusk — not cold blue/purple)
private val TamixaDarkBg = WarmCharcoal
private val TamixaSurface = WarmSurface
private val TamixaSurfaceVariant = WarmSurfaceVariant
private val TamixaBorder = Color(0xFF3A3630)
private val Cream = WarmSand
private val GrayText = Color(0xFFDDD8CE)       // Slightly brighter on dark — less “muddy” body text
private val LavenderGlow = Color(0xFFEDE8F5)  // Kept for tertiary
private val ErrorRed = Color(0xFFE57373)
private val OnError = Color(0xFF2D1520)

// Primary = Deep Teal; accent = Terracotta (Storybook Dusk)
private val NightSkyBg = TamixaDarkBg
private val NightSkySurface = TamixaSurface
private val NightSkySurfaceVariant = TamixaSurfaceVariant
private val OnNightSky = Cream
private val OnNightSkyVariant = WarmSand.copy(alpha = 0.9f)
private val StarAccent = Terracotta
private val CloudLight = WarmSand.copy(alpha = 0.6f)

// Light mode: warm cream (Storybook Dusk — not cold sky blue)
private val AppBgTop = Color(0xFFFDF8F5)      // warm cream
private val AppBgMid = Color(0xFFF5EDE4)      // warm sand
private val AppBgBottom = Color(0xFFEDE4D8)   // warm beige
private val AppHeading = Color(0xFF2C2520)    // warm charcoal
/** Slightly deeper than before — clearer hierarchy on cream (Vocal Image–style crisp readability). */
private val AppTextSecondary = Color(0xFF4A4238) // muted warm gray

// Light theme: starry night background uses cream text (titles/headings)
private val LightBg = AppBgTop
private val LightSurface = AppBgMid
private val LightSurfaceVariant = AppBgBottom
private val OnCream = AppHeading
private val OnSurfaceVariantLight = AppTextSecondary
/** Warm neutral stroke — avoids cool blue fringe on Storybook Dusk cream (cleaner, more “pro app”). */
private val OutlineLight = Color(0xFFC9B8A8)

private val NightSkyScheme = darkColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A3A),
    onPrimaryContainer = Color(0xFFE8F5F3),
    secondary = StarAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3D2B28),
    onSecondaryContainer = Color(0xFFFFE8E4),
    tertiary = CloudLight,
    onTertiary = OnNightSky,
    tertiaryContainer = TamixaSurfaceVariant,
    onTertiaryContainer = OnNightSkyVariant,
    error = ErrorRed,
    onError = OnError,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = ErrorRed,
    background = TamixaDarkBg,
    onBackground = Cream,
    surface = TamixaSurface,
    onSurface = Cream,
    surfaceVariant = TamixaSurfaceVariant,
    onSurfaceVariant = OnNightSkyVariant,
    outline = Color(0xFF6B6358).copy(alpha = 0.55f),
    outlineVariant = Color(0xFF4A443C)
)

private val LightScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = OnCream,
    secondary = Terracotta,
    onSecondary = Color(0xFF2D2310),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF2D2310),
    tertiary = TealMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5F2F0),
    onTertiaryContainer = Color(0xFF1E3D3D),
    error = ErrorRed,
    onError = OnError,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = LightBg,
    onBackground = OnCream,
    surface = LightSurface,
    onSurface = OnCream,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE7DED4)
)

private val DarkScheme = NightSkyScheme

// Modern M3-style radii — generous corners (2024+ product UI)
private val TamixaShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

/** Relative luminance (0–1) for light/dark detection. WCAG-style. */
internal fun Color.luminance(): Float =
    this.red * 0.299f + this.green * 0.587f + this.blue * 0.114f

/**
 * App-wide UI face: **Plus Jakarta Sans** (variable wght) from `composeResources/font/`.
 * SIL OFL 1.1 — [google/fonts ofl/plusjakartasans](https://github.com/google/fonts/tree/main/ofl/plusjakartasans).
 * [Font] is @Composable (resources); built each [TamixaTheme] recomposition (cheap at app root).
 */
@Composable
private fun tamixaFontFamily(): FontFamily = FontFamily(
    Font(tamixaUiSansFont, FontWeight.Normal, FontStyle.Normal),
    Font(tamixaUiSansFont, FontWeight.Medium, FontStyle.Normal),
    Font(tamixaUiSansFont, FontWeight.SemiBold, FontStyle.Normal),
    Font(tamixaUiSansFont, FontWeight.Bold, FontStyle.Normal),
    Font(tamixaUiSansFont, FontWeight.ExtraBold, FontStyle.Normal),
)

// Design spec: cream text on dark, dark text on light surfaces
private fun tamixaTypography(fontFamily: FontFamily): Typography = Typography(
    // Tuned for Plus Jakarta Sans + dense mobile layouts: clear title/subtitle steps, lineHeight ≥ ~1.25× size.
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.2).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.15).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.1).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    /** Rare full-width heroes (onboarding). */
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.2).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    /** Login / OTP screen titles, secondary heroes. */
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.15).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    /** Dashboard greeting, player title — fits beside 56dp chrome. */
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.12).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    /** Section headers (glass rails), empty states. */
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.08).sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    /** App bar, list row titles, dialogs. */
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.02.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.04.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    /** Subtitle under headlines (paired with titleLarge / headline*). */
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.12.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.12.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.18.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.08.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.28.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.38.sp,
        platformStyle = TamixaCrispPlatformTextStyle
    )
)

@Composable
fun TamixaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NightSkyScheme else LightScheme
    val fontFamily = tamixaFontFamily()
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = TamixaShapes,
        typography = tamixaTypography(fontFamily),
        content = content
    )
}

/**
 * Shared chrome (top/bottom bars) tied to the active [MaterialTheme] color scheme so light/dark
 * stay coherent instead of hard-coded charcoal everywhere.
 */
object TamixaChrome {
    /**
     * Top bar fill on starfield / dark screens — translucent “glass” so the sky shows through slightly
     * (modern docked header, not a flat slab).
     */
    @Composable
    fun barContainerColor(): Color = dockedChromeFill(variant = ChromeFillVariant.TopBar)

    /**
     * Floating bottom nav pill — same [ColorScheme] base as the top bar, slightly more opaque so
     * labels and taps stay readable over the starfield.
     */
    @Composable
    fun bottomBarContainerColor(): Color = dockedChromeFill(variant = ChromeFillVariant.BottomBar)

    /** Subtle rim for the floating bottom bar; matches light/dark starfield chrome. */
    @Composable
    fun chromeOutlineColor(): Color {
        val scheme = MaterialTheme.colorScheme
        return if (scheme.background.luminance() < 0.5f) {
            Color.White.copy(alpha = 0.16f)
        } else {
            Color(0xFF2C2520).copy(alpha = 0.12f)
        }
    }

    private enum class ChromeFillVariant { TopBar, BottomBar }

    @Composable
    private fun dockedChromeFill(variant: ChromeFillVariant): Color {
        val scheme = MaterialTheme.colorScheme
        val isDark = scheme.background.luminance() < 0.5f
        return when {
            !isDark && variant == ChromeFillVariant.TopBar ->
                scheme.surfaceContainer.copy(alpha = 0.96f)
            !isDark ->
                scheme.surface.copy(alpha = 0.98f)
            variant == ChromeFillVariant.TopBar ->
                scheme.surface.copy(alpha = 0.78f)
            else ->
                scheme.surface.copy(alpha = 0.88f)
        }
    }
}

// Design system: named colors for gradients, glows, components (aligned with admin/web)
// Use TamixaColors.cream / lavenderGlow only for text on dark (screen) backgrounds (e.g. headlines).
// For text inside cards, use TamixaContentColors.cardPrimary() / cardSecondary() so it stays visible in all themes.
object TamixaColors {
    // Figma wireframe palette
    val warmOrange: Color get() = FigmaWarmOrange
    val skyBlue: Color get() = FigmaSkyBlue
    val softPurple: Color get() = FigmaSoftPurple
    val mintGreen: Color get() = FigmaMintGreen
    val lightBackground: Color get() = FigmaLightBg
    val blue: Color get() = TamixaBlue
    val purple: Color get() = TamixaPurple
    val yellow: Color get() = TamixaYellow
    val accent: Color get() = Terracotta
    val terracotta: Color get() = Terracotta
    val deepTeal: Color get() = DeepTeal
    val orange: Color get() = TamixaOrange
    val pink: Color get() = TamixaPink
    val indigo: Color get() = TamixaIndigo
    /** Primary CTA / active tab: terracotta (Storybook Dusk — distinctive, warm) */
    val goldAccent: Color get() = Terracotta
    val goldAccentLight: Color get() = TamixaOrange
    val creamBackground: Color get() = TamixaDarkBg
    val creamSurface: Color get() = TamixaSurface
    val warmSurfaceVariant: Color get() = TamixaSurfaceVariant
    val yellowOrangePrimary: Color get() = WarmAmber
    val yellowOrangeSecondary: Color get() = TamixaOrange
    val nightSkyBg: Color get() = TamixaDarkBg
    val nightSkySurface: Color get() = TamixaSurface
    /** White rounded input surface for night sky screens */
    val inputSurface: Color get() = Color(0xFFFFFFFF)
    /** Text color on white inputs (dark for contrast) */
    val onInputSurface: Color get() = Color(0xFF2C2520)
    val nightSkySurfaceVariant: Color get() = TamixaSurfaceVariant
    /** Deeper warm charcoal for player chrome / docks; main tab bar uses [nightSkyBg] in [com.tamixa.ui.components.TamixaBottomBar]. */
    val bottomBarBackgroundDark: Color get() = Color(0xFF0F0D09)
    /**
     * Mid-tone anchor for light bottom nav — warm taupe between cream surfaces and shadows.
     */
    val bottomBarBackgroundLight: Color get() = Color(0xFFDDD3C8)
    val cream: Color get() = Cream
    val lavenderGlow: Color get() = LavenderGlow
    val grayText: Color get() = GrayText
    /** Alias for primary (design system purple) */
    val maroonPrimary: Color get() = TamixaPurple
    /** App background gradient — sky blue family */
    val appBgTop: Color get() = AppBgTop
    val appBgMid: Color get() = AppBgMid
    val appBgBottom: Color get() = AppBgBottom
    val appHeading: Color get() = AppHeading
    val appTextSecondary: Color get() = AppTextSecondary
    /** Mint accent from edu-story gradients — highlights on dark UIs, success-adjacent checks. */
    val eduStoryMint: Color get() = EduStoryMint
}

/** Onboarding flow: glassy cards on starfield — high contrast, calm modern polish */
object OnboardingCardColors {
    /** Frosted light card — higher opacity so busy starfield doesn’t read as “dirty” behind UI */
    val cardBackground = Color.White.copy(alpha = 0.68f)
    /** Frame on frosted cards — Learn / edu-story forward (teal), pairs with terracotta labels. */
    val cardBorder = DeepTeal.copy(alpha = 0.38f)
    /** Default pill: warm white tint */
    val pillBackground = Color.White.copy(alpha = 0.62f)
    /** Text on cards and pills — strong contrast for elder readability */
    val onCardText = Color(0xFF252035)
    /** Label/title on cards — warm terracotta, inviting on frosted surfaces */
    val cardLabel = Color(0xFFC4625A)
    /** Section headlines on dark onboarding background */
    val onboardingHeadline = Color(0xFFF8F2E8)
    /** Section sublines */
    val onboardingSubline = Color(0xFFE8E0D8)
    /** Progress header: light frosted strip */
    val progressHeaderBackground = Color.White.copy(alpha = 0.55f)
    val progressHeaderText = Color(0xFF3D3552)
    val progressTrack = Terracotta.copy(alpha = 0.35f)
    /** Accent pills for variety (Storybook Dusk) */
    val pillWarmOrange = Terracotta.copy(alpha = 0.85f)
    val pillSoftPurple = DeepTeal.copy(alpha = 0.75f)
    val pillMint = Color(0xFF7ED9C4).copy(alpha = 0.85f)
    /** Learn / life-skills lane chips on onboarding (distinct from Fun pills). */
    val pillLearnLane = DeepTeal.copy(alpha = 0.88f)
    /** Progress chip on starfield */
    val progressChipFill = Color.White.copy(alpha = 0.10f)
    val progressChipBorder = Color.White.copy(alpha = 0.22f)
    val progressTrackDim = Color.White.copy(alpha = 0.14f)
}

/** Shared onboarding card elevation — lighter shadow = cleaner edges, less gray haze */
object OnboardingCardDefaults {
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val cardShadowElevation = 8.dp
    val cardShadowAmbient = Color.Black.copy(alpha = 0.12f)
    val cardShadowSpot = Color.Black.copy(alpha = 0.07f)
}

/** Design spec tokens: card radii, spacing, elevation — premium HD feel, kid- and elder-friendly */
object TamixaDesignTokens {
    val cardElevation = 6.dp
    val cardElevationHover = 10.dp
    val dialogRadius = 28.dp
    val buttonRadius = 28.dp
    val inputRadius = 20.dp
    /** Card corner radius: list/content cards — consistent professional look */
    val cardRadius = 18.dp
    /** List rows / compact cards — between default and large feature cards */
    val cardRadiusMedium = 20.dp
    /** Large feature cards (e.g. profile menu, settings sections) */
    val cardRadiusLarge = 22.dp
    /** Carousel/poster cards */
    val carouselCardRadius = 18.dp
    /** List row cards: soft shadow (ambient / spot) — use with [listCardShadowElevation] */
    val listCardShadowElevation = 5.dp
    val listCardShadowAmbient = Color.Black.copy(alpha = 0.055f)
    val listCardShadowSpot = Color.Black.copy(alpha = 0.04f)
    /** Poster / carousel cards: slightly deeper shadow */
    val carouselCardShadowElevation = 8.dp
    val carouselCardShadowAmbient = Color.Black.copy(alpha = 0.075f)
    val carouselCardShadowSpot = Color.Black.copy(alpha = 0.055f)
    /** Screen edge padding — consistent on all screens */
    val screenPadding = 24.dp
    /** Extra bottom padding when screen has bottom nav — minimal gap above bar, consistent across app */
    val screenPaddingBottomWithNav = 0.dp
    /** Bottom inset when there is no bottom navigation */
    val screenPaddingBottomWithoutNav = 28.dp
    val headerPaddingHorizontal = 20.dp
    val headerPaddingVertical = 18.dp
    val contentPaddingHorizontal = 20.dp
    /** Vertical spacing between sections — clear hierarchy */
    val sectionSpacing = 24.dp
    val cardSpacing = 16.dp
    /** Compact spacing (e.g. list items, benefit rows, in-row gaps) — consistent professional look */
    val smallSpacing = 12.dp
    /** Padding inside cards — comfortable for touch and read */
    val cardContentPadding = 20.dp
    /** Min touch target (accessibility); use for icon buttons and list rows */
    val minTouchTargetSize = 48.dp
    /** Full-width hero art on story player — consistent height across devices */
    val storyIllustrationHeroHeight = 264.dp
    /** Corner radius for player hero illustration frame (cards may use [cardRadius] or [carouselCardRadius]) */
    val storyIllustrationFrameRadius = 18.dp
    /** Full-width home hero: flat top, rounded bottom (aligned with illustration frame). */
    val heroBannerShape: androidx.compose.foundation.shape.RoundedCornerShape
        get() = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = storyIllustrationFrameRadius,
            bottomEnd = storyIllustrationFrameRadius,
        )
    /** Soft glow for primary CTA (gold) */
    val buttonShadowElevation = 8.dp
    val fabShadowElevation = 12.dp
    /** Vertical gap inside starfield hub blocks (dashboard carousels, library generate + filters). */
    val starfieldHubItemSpacing = 10.dp
}

/** Shared styling for dialogs: shape and surface for kid + parent appeal. */
object TamixaDialogDefaults {
    val shape get() = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.dialogRadius)
}

object TamixaGradients {
    /** Primary CTA: Figma warm orange gradient */
    val primaryButton = listOf(FigmaWarmOrange, Color(0xFFFF6B35))
    /** Splash / hero: Figma sky blue → soft purple */
    val splashBackground = listOf(FigmaSkyBlue, FigmaSoftPurple, Color(0xFF8B7FD4))
    /**
     * Onboarding: professional, powerful, impactful, entertaining.
     * Deep navy → rich indigo → royal violet → deep plum with subtle warmth.
     * Premium jewel tones that feel cinematic and memorable.
     */
    val onboardingBackground = listOf(
        Color(0xFF0a0a14),
        Color(0xFF12101e),
        Color(0xFF1a1628),
        Color(0xFF25203a),
        Color(0xFF2d2448),
        Color(0xFF2a2342),
        Color(0xFF1f182e)
    )
    /** Reward / accent: Terracotta → warm coral */
    val rewardButton = listOf(Terracotta, Color(0xFFD47A6F))
    /** Alias for screens that expect a dark background list */
    val lightBackgroundWithAccent: List<Color> get() = nightSkyBackground
    /** Storybook Dusk: warm charcoal gradient (no cold blue) */
    val nightSkyBackground = listOf(
        Color(0xFF0E0C0A),
        TamixaDarkBg,
        Color(0xFF1C1815)
    )
    val imageOverlay = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.3f),
        Color.Black.copy(alpha = 0.85f)
    )
    /** App background gradient — warm cream (Storybook Dusk) */
    val appBackground = listOf(
        Color(0xFFFDF8F5),
        Color(0xFFF5EDE4),
        Color(0xFFEDE4D8)
    )
    fun appBackgroundBrush() = Brush.verticalGradient(appBackground)
    /** Primary button gradient brush (Figma warm orange) */
    fun primaryButtonBrush() = Brush.linearGradient(primaryButton)

    /**
     * “Storybook playbook” shelf frame — terracotta → teal → mint edge (chunky kid-app energy, not Duolingo green).
     */
    fun storybookShelfFrameBrush(): Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Terracotta.copy(alpha = 0.98f),
            0.48f to DeepTeal.copy(alpha = 0.88f),
            1f to EduStoryMint.copy(alpha = 0.92f)
        ),
        start = Offset.Zero,
        end = Offset(1080f, 520f)
    )

    /**
     * Onboarding editorial cards — Learn / edu-story forward (teal spine energy, still warm).
     */
    fun onboardingEduStoryFrameBrush(): Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to DeepTeal.copy(alpha = 0.96f),
            0.42f to Terracotta.copy(alpha = 0.82f),
            1f to EduStoryMint.copy(alpha = 0.9f)
        ),
        start = Offset.Zero,
        end = Offset(920f, 480f)
    )

    /** Splash: strong bottom wash so launch feels branded and alive immediately. */
    fun splashStorybookVignetteBrush(): Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.Transparent,
            0.5f to Color.Transparent,
            0.78f to Terracotta.copy(alpha = 0.26f),
            1f to DeepTeal.copy(alpha = 0.32f)
        )
    )

    /** Onboarding: soft top atmosphere — wider fade, less “flat header band”. */
    fun onboardingTopAtmosphereBrush(): Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Terracotta.copy(alpha = 0.11f),
            0.28f to DeepTeal.copy(alpha = 0.06f),
            0.55f to Color.Transparent,
            1f to Color.Transparent
        ),
        startY = 0f,
        endY = 1400f
    )
    /** Splash gradient brush */
    fun splashBackgroundBrush() = Brush.verticalGradient(splashBackground)
    /** Reward / star gradient brush */
    val yellowOrangeButton = listOf(WarmAmber, TamixaOrange)
    fun yellowOrangeButtonBrush() = Brush.verticalGradient(yellowOrangeButton)
}

/**
 * Global card colors so text is always visible. Use these instead of ad-hoc containerColor
 * so contentColor is set and cards stay consistent across the app.
 */
object TamixaCardColors {
    @Composable
    fun surface() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    @Composable
    fun primaryContainer() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    @Composable
    fun secondaryContainer() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    @Composable
    fun surfaceVariant() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    @Composable
    fun errorContainer() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )
}

/**
 * Use for text on cards so it stays visible. Prefer these over TamixaColors.cream/lavenderGlow
 * inside cards, since those are for text on dark (screen) backgrounds only.
 */
object TamixaContentColors {
    @Composable
    fun cardPrimary() = MaterialTheme.colorScheme.onSurface
    @Composable
    fun cardSecondary() = MaterialTheme.colorScheme.onSurfaceVariant
    @Composable
    fun onPrimaryContainer() = MaterialTheme.colorScheme.onPrimaryContainer
    @Composable
    fun onSecondaryContainer() = MaterialTheme.colorScheme.onSecondaryContainer
    @Composable
    fun onErrorContainer() = MaterialTheme.colorScheme.onErrorContainer
}

/** Design system colors for components (admin/web aligned) */
object TamixaDesignColors {
    val blue = TamixaBlue
    val purple = TamixaPurple
    val yellow = TamixaYellow
    val accent = Terracotta
    val orange = TamixaOrange
    val pink = TamixaPink
    val indigo = TamixaIndigo
    val cream = Cream
    val lavenderGlow = LavenderGlow
    val softBlue = TamixaBlue
    val softGold = Terracotta
}
