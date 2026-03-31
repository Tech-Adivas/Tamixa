package com.tamixa.ui.components

actual fun prefetchStoryCover(url: String?) {
    // Coil cache prefetch is wired on Android first; iOS relies on normal AsyncImage decode.
}
