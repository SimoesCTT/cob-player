package com.cobplayer.app.audio

import android.media.audiofx.Equalizer
import androidx.media3.common.util.UnstableApi

@UnstableApi
class EqualizerController {

    private var eq: Equalizer? = null
    private var wantEnabled: Boolean = false

    fun attach(audioSessionId: Int) {
        release()
        try {
            eq = Equalizer(0, audioSessionId).apply {
                this.enabled = wantEnabled
            }
        } catch (e: Exception) {
            eq = null
        }
    }

    val isAvailable: Boolean get() = eq != null

    var enabled: Boolean
        get() = wantEnabled
        set(v) {
            wantEnabled = v
            try { eq?.enabled = v } catch (_: Exception) {}
        }

    fun presets(): List<Pair<String, Short>> {
        val e = eq ?: return emptyList()
        val count = e.numberOfPresets.toInt()
        return (0 until count).map { i ->
            e.getPresetName(i.toShort()) to i.toShort()
        }
    }

    fun currentPreset(): Short? = eq?.currentPreset

    fun usePreset(id: Short) {
        try { eq?.usePreset(id) } catch (_: Exception) {}
    }

    fun bandRange(): Pair<Short, Short>? {
        val e = eq ?: return null
        val range = e.bandLevelRange
        return range[0] to range[1]
    }

    fun bandCount(): Int = eq?.numberOfBands?.toInt() ?: 0

    fun bandFreqHz(band: Int): Int {
        val e = eq ?: return 0
        return try { e.getCenterFreq(band.toShort()) / 1000 } catch (_: Exception) { 0 }
    }

    fun bandLevel(band: Int): Short =
        try { eq?.getBandLevel(band.toShort()) ?: 0 } catch (_: Exception) { 0 }

    fun setBandLevel(band: Int, level: Short) {
        try { eq?.setBandLevel(band.toShort(), level) } catch (_: Exception) {}
    }

    fun release() {
        try { eq?.release() } catch (_: Exception) {}
        eq = null
    }
}
