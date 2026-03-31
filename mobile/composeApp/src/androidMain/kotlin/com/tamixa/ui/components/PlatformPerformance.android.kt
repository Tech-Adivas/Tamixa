package com.tamixa.ui.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of performance detection.
 */

@Composable
actual fun platformIsReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return try {
        val animationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        animationScale == 0f
    } catch (e: Exception) {
        false
    }
}

@Composable
actual fun platformIsLowEndDevice(): Boolean {
    val context = LocalContext.current
    
    // Check if device is low RAM
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val isLowRam = activityManager?.isLowRamDevice ?: false
    
    if (isLowRam) return true
    
    // Check available RAM (< 3GB = low-end)
    val memInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memInfo)
    val totalRamGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    
    if (totalRamGB < 3.0) return true
    
    // Check CPU cores (< 4 cores = low-end)
    val cpuCores = Runtime.getRuntime().availableProcessors()
    if (cpuCores < 4) return true
    
    // Check Android version (< Android 8 = low-end)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    
    return false
}
