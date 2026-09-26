package com.tamixa.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamixa.composeapp.generated.resources.Res
import com.tamixa.composeapp.generated.resources.onboarding_demo_hero
import com.tamixa.composeapp.generated.resources.onboarding_demo_hero_animated
import com.tamixa.ui.components.TamixaPrimaryButton
import com.tamixa.ui.components.TamixaSkipButton
import com.tamixa.ui.strings.Strings
import com.tamixa.ui.theme.OnboardingCardColors
import com.tamixa.ui.theme.OnboardingCardDefaults
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingInteractivePreviewScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onSwipeToNext: (() -> Unit)? = null,
    onSwipeToPrevious: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Debug logging
    LaunchedEffect(Unit) {
        println("🎯 OnboardingInteractivePreviewScreen COMPOSED - Step 3 of 5")
    }
    
    OnboardingShell(
        step = 3,
        totalSteps = 5,
        modifier = modifier,
        onSwipeToNext = onSwipeToNext,
        onSwipeToPrevious = onSwipeToPrevious,
        footer = {
            val spec = LocalOnboardingLayoutSpec.current
            TamixaPrimaryButton(
                onClick = onContinue,
                text = Strings.continueLabel(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onboardingEntrance(delayMs = 280)
            )
            Spacer(Modifier.height(spec.footerButtonGap))
            TamixaSkipButton(
                onClick = onSkip,
                text = Strings.skip(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onboardingEntrance(delayMs = 320)
            )
        }
    ) {
        val spec = LocalOnboardingLayoutSpec.current
        
        // Compact headline
        Text(
            text = Strings.onboardingInteractiveHeadline(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp,
                lineHeight = 32.sp
            ),
            color = TamixaColors.cream,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onboardingEntrance(delayMs = 55)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Compact interactive card
        InteractiveStoryPreviewCard(
            modifier = Modifier.onboardingEntrance(delayMs = 115)
        )
    }
}

@Composable
private fun InteractiveStoryPreviewCard(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // State for choice selection and animation
    var selectedChoice by remember { mutableStateOf<Int?>(null) }
    var showBranchingAnimation by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Compact story scenario card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = TamixaColors.goldAccent.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compact emoji illustration
                Text(
                    text = "🦊",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 48.sp
                )
                
                // Compact story question
                Text(
                    text = Strings.onboardingInteractiveCardTeaser(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 26.sp
                    ),
                    color = TamixaColors.appHeading,
                    textAlign = TextAlign.Center
                )
                
                // Compact branching visualization
                BranchingPathVisualization(
                    selectedChoice = selectedChoice,
                    showAnimation = showBranchingAnimation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
        
        // Compact choice buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernChoiceButton(
                text = Strings.onboardingInteractiveChoiceA(),
                emoji = "🌲",
                color = TamixaColors.eduStoryMint,
                isSelected = selectedChoice == 0,
                onClick = {
                    selectedChoice = 0
                    showBranchingAnimation = true
                    scope.launch {
                        delay(2000)
                        showBranchingAnimation = false
                        selectedChoice = null
                    }
                }
            )
            
            ModernChoiceButton(
                text = Strings.onboardingInteractiveChoiceB(),
                emoji = "🏘️",
                color = TamixaColors.terracotta,
                isSelected = selectedChoice == 1,
                onClick = {
                    selectedChoice = 1
                    showBranchingAnimation = true
                    scope.launch {
                        delay(2000)
                        showBranchingAnimation = false
                        selectedChoice = null
                    }
                }
            )
        }
    }
}

@Composable
private fun ModernChoiceButton(
    text: String,
    emoji: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "choiceScale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 2f else 8f,
        animationSpec = tween(durationMillis = 200),
        label = "choiceElevation"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = color.copy(alpha = 0.3f)
            )
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) {
            color.copy(alpha = 0.15f)
        } else {
            Color.White.copy(alpha = 0.9f)
        },
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 2.dp,
            color = if (isSelected) color else color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(color = color)
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Compact emoji in colored circle
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 24.sp
                    )
                }
            }
            
            // Compact choice text
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = 0.sp
                ),
                color = TamixaColors.appHeading,
                modifier = Modifier.weight(1f)
            )
            
            // Arrow indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractiveChoiceButton(
    text: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "choiceScale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 2f else 6f,
        animationSpec = tween(durationMillis = 200),
        label = "choiceElevation"
    )
    
    val borderColor = if (isSelected) {
        TamixaColors.goldAccent.copy(alpha = 0.9f)
    } else {
        TamixaColors.deepTeal.copy(alpha = 0.4f)
    }
    
    val backgroundColor = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(
                TamixaColors.goldAccent.copy(alpha = 0.2f),
                TamixaColors.eduStoryMint.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(TamixaDesignTokens.cardRadius),
                ambientColor = if (isSelected) TamixaColors.goldAccent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
            )
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(TamixaDesignTokens.cardRadius),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.5.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(color = TamixaColors.goldAccent)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Emoji with subtle animation
                val emojiScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "emojiScale"
                )
                
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            scaleX = emojiScale
                            scaleY = emojiScale
                        }
                )
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = if (isSelected) {
                        OnboardingCardColors.onCardText
                    } else {
                        OnboardingCardColors.onCardText.copy(alpha = 0.88f)
                    },
                    modifier = Modifier.weight(1f)
                )
                
                if (isSelected) {
                    // Animated arrow
                    val arrowOffset by animateFloatAsState(
                        targetValue = 8f,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        ),
                        label = "arrowOffset"
                    )
                    
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TamixaColors.goldAccent,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = arrowOffset.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BranchingPathVisualization(
    selectedChoice: Int?,
    showAnimation: Boolean,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    
    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1400,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        } else {
            animProgress.snapTo(0f)
        }
    }
    
    Box(
        modifier = modifier
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Central node - larger and more prominent with glow
        Box(
            modifier = Modifier
                .size(20.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = TamixaColors.goldAccent.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TamixaColors.goldAccent,
                            TamixaColors.deepTeal
                        )
                    )
                )
                .align(Alignment.CenterStart)
                .offset(x = 60.dp)
        )
        
        // Branch paths
        if (selectedChoice != null && showAnimation) {
            val targetY = if (selectedChoice == 0) -40.dp else 40.dp
            val progress = animProgress.value
            
            // Animated path line - thicker and more visible with gradient
            Box(
                modifier = Modifier
                    .width((160 * progress).dp)
                    .height(4.dp)
                    .offset(x = 80.dp, y = targetY * progress)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(2.dp),
                        ambientColor = TamixaColors.goldAccent.copy(alpha = 0.3f)
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TamixaColors.goldAccent,
                                TamixaColors.eduStoryMint
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            // Animated particle effect along the path
            if (progress > 0.3f) {
                val particleProgress = (progress - 0.3f) / 0.7f
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .align(Alignment.CenterStart)
                        .offset(
                            x = (80 + 160 * particleProgress).dp,
                            y = targetY * progress
                        )
                )
            }
            
            // End node - larger and more prominent with pulse
            if (progress > 0.7f) {
                val endNodeScale = ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = TamixaColors.eduStoryMint.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TamixaColors.eduStoryMint,
                                    TamixaColors.goldAccent.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .align(Alignment.CenterStart)
                        .offset(x = 222.dp, y = targetY)
                        .graphicsLayer {
                            alpha = endNodeScale
                            scaleX = endNodeScale
                            scaleY = endNodeScale
                        }
                )
            }
        } else {
            // Static branching preview - more visible and elegant
            // Top branch
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.5.dp)
                    .offset(x = 80.dp, y = (-32).dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TamixaColors.deepTeal.copy(alpha = 0.5f),
                                TamixaColors.deepTeal.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(TamixaColors.deepTeal.copy(alpha = 0.5f))
                    .align(Alignment.CenterStart)
                    .offset(x = 190.dp, y = (-32).dp)
            )
            
            // Bottom branch
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.5.dp)
                    .offset(x = 80.dp, y = 32.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TamixaColors.deepTeal.copy(alpha = 0.5f),
                                TamixaColors.deepTeal.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(TamixaColors.deepTeal.copy(alpha = 0.5f))
                    .align(Alignment.CenterStart)
                    .offset(x = 190.dp, y = 32.dp)
            )
        }
    }
}
