package com.tamixa.ui.components

/**
 * Warm the disk/memory cache before opening the player (Phase 1 foundation).
 * No-op on platforms without a shared image pipeline.
 */
expect fun prefetchStoryCover(url: String?)
