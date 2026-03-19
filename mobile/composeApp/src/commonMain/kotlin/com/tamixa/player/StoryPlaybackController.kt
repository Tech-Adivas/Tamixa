package com.tamixa.player

/** Result of avatar video controller: playback controller + platform player (ExoPlayer/AVPlayer). */
data class AvatarVideoControllerResult(
    val controller: StoryPlaybackController,
    val player: Any?
)

/**
 * Platform-agnostic story playback controller.
 * Android: ExoPlayer/MediaPlayer/TTS. iOS: AVPlayer/AVSpeechSynthesizer.
 */
interface StoryPlaybackController {
    /** True when the player has loaded and playback can start. Restrict play until true. */
    val isReady: Boolean
    val isPlaying: Boolean
    val progress: Float
    fun playPause()
    fun rewind()
    fun fastForward()
    fun setSleepTimer(minutes: Int)
    fun share()
    fun download(url: String, filename: String, mimeType: String = "audio/mpeg")
}
