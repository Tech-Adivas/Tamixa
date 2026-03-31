package com.tamixa.ui.components

import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.tamixa.platform.tamixaApplicationContextOrNull

actual fun prefetchStoryCover(url: String?) {
    val u = url?.trim()?.takeIf { it.isNotEmpty() } ?: return
    val ctx = tamixaApplicationContextOrNull() ?: return
    val loader = SingletonImageLoader.get(ctx)
    val request = ImageRequest.Builder(ctx)
        .data(u)
        .build()
    loader.enqueue(request)
}
