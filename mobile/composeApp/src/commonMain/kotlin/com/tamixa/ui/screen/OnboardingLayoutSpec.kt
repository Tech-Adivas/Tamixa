package com.tamixa.ui.screen

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive onboarding spacing and type — derived from screen size.
 * The shell body is vertically scrollable so content can use comfortable typography without clipping.
 */
@Immutable
data class OnboardingLayoutSpec(
    val heroScale: Float,
    val gapXs: Dp,
    val gapSm: Dp,
    val gapMd: Dp,
    val gapLg: Dp,
    val cardContentPadding: Dp,
    val cardInnerSpacing: Dp,
    val benefitRowVerticalPadding: Dp,
    val benefitRowSpacing: Dp,
    val benefitEmojiLineHeight: TextUnit,
    val editorialHPadding: Dp,
    val editorialVPadding: Dp,
    val editorialSectionGap: Dp,
    val headlineLineHeight: TextUnit,
    val headlineMaxLines: Int,
    val sublineLineHeight: TextUnit,
    val sublineMaxLines: Int,
    val bodyColumnBottomPadding: Dp,
    val footerBottomPadding: Dp,
    val footerButtonGap: Dp,
    val progressPaddingTop: Dp,
    val progressPaddingBottom: Dp,
    val topAtmosphereHeight: Dp,
    val demoPlayButtonDp: Dp,
    val demoPlayIconDp: Dp,
    val valueStripLetterSpacing: Float,
    val homePreviewRowVerticalPadding: Dp,
    val homePreviewCardPadding: Dp,
    val homePreviewInnerSpacing: Dp,
) {
    fun scaledImageHeight(base: Dp): Dp =
        (base.value * heroScale).dp.coerceIn(138.dp, base)

    fun scaledHaloHeight(base: Dp): Dp =
        (base.value * heroScale).dp.coerceIn(198.dp, base)

    fun scaledCorner(base: Dp): Dp =
        (base.value * (0.92f + 0.08f * heroScale)).dp.coerceIn(14.dp, base)
}

fun onboardingLayoutSpecForScreen(maxHeight: Dp, maxWidth: Dp): OnboardingLayoutSpec {
    val h = maxHeight.value
    val narrow = maxWidth < 340.dp
    val base = when {
        h < 560f -> tierVeryCompact()
        h < 620f -> tierCompact()
        h < 700f -> tierCozy()
        else -> tierRegular()
    }
    return if (narrow) {
        base.copy(
            headlineMaxLines = base.headlineMaxLines.coerceAtMost(3),
            sublineMaxLines = base.sublineMaxLines.coerceAtMost(6),
        )
    } else {
        base
    }
}

private fun tierVeryCompact() = OnboardingLayoutSpec(
    heroScale = 0.8f,
    gapXs = 4.dp,
    gapSm = 6.dp,
    gapMd = 8.dp,
    gapLg = 10.dp,
    cardContentPadding = 10.dp,
    cardInnerSpacing = 6.dp,
    benefitRowVerticalPadding = 8.dp,
    benefitRowSpacing = 6.dp,
    benefitEmojiLineHeight = 28.sp,
    editorialHPadding = 14.dp,
    editorialVPadding = 10.dp,
    editorialSectionGap = 6.dp,
    headlineLineHeight = 30.sp,
    headlineMaxLines = 3,
    sublineLineHeight = 21.sp,
    sublineMaxLines = 5,
    bodyColumnBottomPadding = 4.dp,
    footerBottomPadding = 12.dp,
    footerButtonGap = 8.dp,
    progressPaddingTop = 4.dp,
    progressPaddingBottom = 6.dp,
    topAtmosphereHeight = 168.dp,
    demoPlayButtonDp = 50.dp,
    demoPlayIconDp = 26.dp,
    valueStripLetterSpacing = 1.8f,
    homePreviewRowVerticalPadding = 8.dp,
    homePreviewCardPadding = 8.dp,
    homePreviewInnerSpacing = 6.dp,
)

private fun tierCompact() = OnboardingLayoutSpec(
    heroScale = 0.88f,
    gapXs = 5.dp,
    gapSm = 8.dp,
    gapMd = 10.dp,
    gapLg = 12.dp,
    cardContentPadding = 12.dp,
    cardInnerSpacing = 8.dp,
    benefitRowVerticalPadding = 10.dp,
    benefitRowSpacing = 8.dp,
    benefitEmojiLineHeight = 30.sp,
    editorialHPadding = 16.dp,
    editorialVPadding = 12.dp,
    editorialSectionGap = 8.dp,
    headlineLineHeight = 32.sp,
    headlineMaxLines = 4,
    sublineLineHeight = 22.sp,
    sublineMaxLines = 5,
    bodyColumnBottomPadding = 6.dp,
    footerBottomPadding = 16.dp,
    footerButtonGap = 10.dp,
    progressPaddingTop = 6.dp,
    progressPaddingBottom = 8.dp,
    topAtmosphereHeight = 188.dp,
    demoPlayButtonDp = 54.dp,
    demoPlayIconDp = 28.dp,
    valueStripLetterSpacing = 2.1f,
    homePreviewRowVerticalPadding = 9.dp,
    homePreviewCardPadding = 10.dp,
    homePreviewInnerSpacing = 8.dp,
)

private fun tierCozy() = OnboardingLayoutSpec(
    heroScale = 0.9f,
    gapXs = 6.dp,
    gapSm = 10.dp,
    gapMd = 12.dp,
    gapLg = 16.dp,
    cardContentPadding = 14.dp,
    cardInnerSpacing = 9.dp,
    benefitRowVerticalPadding = 11.dp,
    benefitRowSpacing = 9.dp,
    benefitEmojiLineHeight = 31.sp,
    editorialHPadding = 17.dp,
    editorialVPadding = 13.dp,
    editorialSectionGap = 9.dp,
    headlineLineHeight = 33.sp,
    headlineMaxLines = 4,
    sublineLineHeight = 23.sp,
    sublineMaxLines = 6,
    bodyColumnBottomPadding = 7.dp,
    footerBottomPadding = 20.dp,
    footerButtonGap = 11.dp,
    progressPaddingTop = 7.dp,
    progressPaddingBottom = 10.dp,
    topAtmosphereHeight = 204.dp,
    demoPlayButtonDp = 56.dp,
    demoPlayIconDp = 29.dp,
    valueStripLetterSpacing = 2.25f,
    homePreviewRowVerticalPadding = 9.dp,
    homePreviewCardPadding = 11.dp,
    homePreviewInnerSpacing = 9.dp,
)

private fun tierRegular() = OnboardingLayoutSpec(
    heroScale = 1f,
    gapXs = 6.dp,
    gapSm = 12.dp,
    gapMd = 14.dp,
    gapLg = 20.dp,
    cardContentPadding = 16.dp,
    cardInnerSpacing = 10.dp,
    benefitRowVerticalPadding = 12.dp,
    benefitRowSpacing = 10.dp,
    benefitEmojiLineHeight = 32.sp,
    editorialHPadding = 18.dp,
    editorialVPadding = 14.dp,
    editorialSectionGap = 10.dp,
    headlineLineHeight = 34.sp,
    headlineMaxLines = 4,
    sublineLineHeight = 24.sp,
    sublineMaxLines = 6,
    bodyColumnBottomPadding = 8.dp,
    footerBottomPadding = 24.dp,
    footerButtonGap = 12.dp,
    progressPaddingTop = 8.dp,
    progressPaddingBottom = 12.dp,
    topAtmosphereHeight = 220.dp,
    demoPlayButtonDp = 58.dp,
    demoPlayIconDp = 30.dp,
    valueStripLetterSpacing = 2.4f,
    homePreviewRowVerticalPadding = 10.dp,
    homePreviewCardPadding = 12.dp,
    homePreviewInnerSpacing = 10.dp,
)

val LocalOnboardingLayoutSpec = staticCompositionLocalOf {
    tierRegular()
}
