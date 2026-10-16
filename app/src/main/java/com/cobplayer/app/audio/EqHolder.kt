package com.cobplayer.app.audio

/**
 * Global handle to the active EqualizerController.
 * Set by PlaybackService when the audio session is ready.
 */
object EqHolder {
    @Volatile
    var controller: EqualizerController? = null
}
