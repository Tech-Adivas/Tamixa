package com.tamixa.ui.components

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo

/**
 * iOS performance hints. Reduce-motion: UIAccessibility interop varies across Kotlin/Native UIKit
 * bindings; returning false keeps visuals enabled (host clip / narrative overlays follow commonMain gates).
 * Low-end: memory and core count only (low-power mode omitted for the same binding reasons).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun platformIsReduceMotionEnabled(): Boolean = false

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun platformIsLowEndDevice(): Boolean {
    val processInfo = NSProcessInfo.processInfo
    val physicalMemoryGB = processInfo.physicalMemory.toDouble() / (1024.0 * 1024.0 * 1024.0)
    if (physicalMemoryGB < 3.0) return true
    val processorCount = processInfo.processorCount.toInt()
    return processorCount < 4
}
