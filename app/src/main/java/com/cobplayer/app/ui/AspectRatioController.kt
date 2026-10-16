package com.cobplayer.app.ui

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
object AspectRatioController {

    enum class Mode(val label: String) {
        FIT("Fit"),
        FILL("Fill"),
        ZOOM("Zoom"),
        FIXED_16_9("16:9"),
        FIXED_4_3("4:3")
    }

    fun apply(playerView: PlayerView, mode: Mode) {
        playerView.resizeMode = when (mode) {
            Mode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            Mode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            Mode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            Mode.FIXED_16_9, Mode.FIXED_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        // Fixed modes require the video surface ratio override
        // (Media3 doesn't expose a per-ratio setter — this is a placeholder
        // for a future ExoPlayer video listener hook)
    }

    fun cycle(current: Mode): Mode {
        val all = Mode.values()
        return all[(current.ordinal + 1) % all.size]
    }
}
