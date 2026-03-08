package com.araro.ios.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.IntSize
import com.araro.util.AraroLog
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Suppress("DEPRECATION") // UIKitView deprecated in favor of InteropView in newer Compose; keep until we upgrade
@Composable
fun AvatarVideoSurfaceIos(
    player: Any?,
    modifier: Modifier
) {
    val avPlayer = player as? AVPlayer
    val layerHolder = remember { mutableListOf<AVPlayerLayer?>() }
    var lastSize by remember { mutableStateOf(IntSize.Zero) }
    AraroLog.d("AvatarVideoIos", "player=${if (avPlayer != null) "non-null" else "null"}")
    Box(
        modifier = modifier
            .background(Color.Black) // Visible area even before video loads
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                if (size != lastSize && size.width > 0 && size.height > 0) {
                    lastSize = size
                    AraroLog.d("AvatarVideoIos", "layout ${size.width}x${size.height}")
                }
            }
    ) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val view = UIView()
                view.setTranslatesAutoresizingMaskIntoConstraints(false)
                view.setBackgroundColor(platform.UIKit.UIColor.blackColor)
                val layer = AVPlayerLayer.playerLayerWithPlayer(avPlayer)
                layer.videoGravity = AVLayerVideoGravityResizeAspect
                view.layer.addSublayer(layer)
                layerHolder.clear()
                layerHolder.add(layer)
                view
            },
            update = { view ->
                val layer = layerHolder.firstOrNull() ?: view.layer.sublayers?.firstOrNull() as? AVPlayerLayer
                layer?.let {
                    it.player = avPlayer
                    view.setNeedsLayout()
                    view.layoutIfNeeded()
                    val frameW = lastSize.width.toDouble().coerceAtLeast(1.0)
                    val frameH = lastSize.height.toDouble().coerceAtLeast(1.0)
                    CATransaction.begin()
                    CATransaction.setValue(true, kCATransactionDisableActions)
                    it.frame = CGRectMake(0.0, 0.0, frameW, frameH)
                    CATransaction.commit()
                }
            }
        )
    }
}
