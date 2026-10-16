package com.cobplayer.app.ui

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

@UnstableApi
class SleepTimer(private val controller: MediaController) {

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var remainingMs: Long = 0
    private var ticker: Runnable? = null
    private var onTick: ((Long) -> Unit)? = null
    private var onFire: (() -> Unit)? = null

    val isActive: Boolean get() = remainingMs > 0

    fun start(ms: Long, onTick: (Long) -> Unit, onFire: () -> Unit) {
        stop()
        remainingMs = ms
        this.onTick = onTick
        this.onFire = onFire
        ticker = object : Runnable {
            override fun run() {
                remainingMs -= 1000
                onTick(remainingMs)
                if (remainingMs <= 0) {
                    controller.pause()
                    onFire()
                    stop()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(ticker!!, 1000)
    }

    fun stop() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
        remainingMs = 0
        onTick = null
        onFire = null
    }
}
