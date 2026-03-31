package com.tamixa.ui.components

import androidx.compose.runtime.Composable

/**
 * Platform-specific performance detection.
 * Used to optimize animations and reduce complexity on low-end devices.
 */

/**
 * Detects if system reduce motion preference is enabled.
 * Returns true if user has requested reduced motion for accessibility.
 */
@Composable
expect fun platformIsReduceMotionEnabled(): Boolean

/**
 * Detects low-end devices to reduce animation complexity.
 * Uses platform-specific heuristics (RAM, CPU cores, etc.)
 */
@Composable
expect fun platformIsLowEndDevice(): Boolean
