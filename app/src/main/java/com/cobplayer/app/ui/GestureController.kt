package com.cobplayer.app.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs

@UnstableApi
class GestureController(
    private val activity: Activity,
    private val player: ExoPlayer,
    private val root: View,
    private val onDoubleTapCenter: () -> Unit,
    private val onSeekPreview: (targetMs: Long) -> Unit,
    private val onSeekCommit: (targetMs: Long) -> Unit,
    private val onFeedback: (String) -> Unit
) {

    private val audioManager =
        activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mode = Mode.NONE
    private var startX = 0f
    private var startY = 0f
    private var startPos = 0L
    private var startVolume = 0
    private var startBrightness = 0f
    private var maxVolume = 1
    private var dragTargetMs = 0L

    private val gestureDetector = GestureDetector(activity,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onDoubleTapCenter()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val w = root.width
                val x = e.x
                when {
                    x < w * 0.35f -> {
                        val t = (player.currentPosition - 10_000).coerceAtLeast(0)
                        player.seekTo(t)
                        onFeedback("⏪ 10s")
                    }
                    x > w * 0.65f -> {
                        val t = (player.currentPosition + 10_000)
                            .coerceAtMost(player.duration.coerceAtLeast(0))
                        player.seekTo(t)
                        onFeedback("⏩ 10s")
                    }
                    else -> onDoubleTapCenter()
                }
                return true
            }
        })

    private enum class Mode { NONE, HORIZONTAL_SEEK, VERTICAL_LEFT, VERTICAL_RIGHT }

    private val touchListener = View.OnTouchListener { _, event ->
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                startPos = player.currentPosition
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                startBrightness = currentBrightness()
                mode = Mode.NONE
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                val dy = event.y - startY

                if (mode == Mode.NONE) {
                    val threshold = 24f
                    if (abs(dx) > threshold || abs(dy) > threshold) {
                        mode = if (abs(dx) > abs(dy)) {
                            Mode.HORIZONTAL_SEEK
                        } else {
                            if (startX < root.width / 2f) Mode.VERTICAL_LEFT
                            else Mode.VERTICAL_RIGHT
                        }
                    }
                }

                when (mode) {
                    Mode.HORIZONTAL_SEEK -> {
                        val dur = player.duration.coerceAtLeast(1)
                        val scale = dur / 1000f / root.width * 90_000f
                        val target = (startPos + (dx * scale)).toLong()
                            .coerceIn(0, dur)
                        dragTargetMs = target
                        onSeekPreview(target)
                    }
                    Mode.VERTICAL_LEFT -> {
                        val delta = -dy / root.height
                        val newB = (startBrightness + delta).coerceIn(0.05f, 1f)
                        setBrightness(newB)
                        onFeedback("☀ ${(newB * 100).toInt()}%")
                    }
                    Mode.VERTICAL_RIGHT -> {
                        val delta = -dy / root.height
                        val newV = (startVolume + delta * maxVolume)
                            .toInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC, newV, 0
                        )
                        onFeedback("🔊 ${(newV * 100 / maxVolume)}%")
                    }
                    Mode.NONE -> {}
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mode == Mode.HORIZONTAL_SEEK) {
                    onSeekCommit(dragTargetMs)
                }
                mode = Mode.NONE
                true
            }
            else -> false
        }
    }

    fun attach() {
        root.setOnTouchListener(touchListener)
    }

    private fun currentBrightness(): Float {
        val lp = activity.window.attributes
        val b = lp.screenBrightness
        return if (b < 0f) 0.5f else b
    }

    private fun setBrightness(v: Float) {
        val lp = activity.window.attributes
        lp.screenBrightness = v
        activity.window.attributes = lp
    }
}
