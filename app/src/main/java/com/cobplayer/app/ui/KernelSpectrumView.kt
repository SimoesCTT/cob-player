package com.cobplayer.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View

/**
 * Renders a spectrum analyzer as vertical "kernels" of corn.
 * Amber bars rising from soil — branded visual feedback.
 */
class KernelSpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var visualizer: Visualizer? = null
    private var waveform: ByteArray? = null
    private var magnitudes: FloatArray? = null
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFC107")
    }
    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE082")
    }
    private val barCount = 24

    fun attach(audioSessionId: Int) {
        detach()
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        v: Visualizer?, waveform: ByteArray?, samplingRate: Int
                    ) { this@KernelSpectrumView.waveform = waveform; postInvalidate() }

                    override fun onFftDataCapture(
                        v: Visualizer?, fft: ByteArray?, samplingRate: Int
                    ) {
                        if (fft == null) return
                        val mags = FloatArray(barCount)
                        val step = fft.size / barCount
                        for (i in 0 until barCount) {
                            var sum = 0f
                            for (j in 0 until step) {
                                val idx = i * step + j
                                if (idx + 1 < fft.size) {
                                    val re = fft[idx].toFloat()
                                    val im = fft[idx + 1].toFloat()
                                    sum += kotlin.math.hypot(re, im)
                                }
                            }
                            mags[i] = sum / step / 128f
                        }
                        magnitudes = mags
                        postInvalidate()
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (_: Exception) { visualizer = null }
    }

    fun detach() {
        try { visualizer?.enabled = false } catch (_: Exception) {}
        try { visualizer?.release() } catch (_: Exception) {}
        visualizer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mags = magnitudes ?: return
        val barWidth = width.toFloat() / barCount
        val minH = 4f

        mags.forEachIndexed { i, m ->
            val h = (m * height).coerceIn(minH, height.toFloat())
            val left = i * barWidth + barWidth * 0.15f
            val right = (i + 1) * barWidth - barWidth * 0.15f
            val top = height - h
            val bottom = height.toFloat()
            canvas.drawRoundRect(left, top, right, bottom, 4f, 4f, barPaint)
            // Kernel cap: small rounded bit on top
            canvas.drawCircle((left + right) / 2f, top + 6f, 6f, capPaint)
        }
    }

    override fun onDetachedFromWindow() {
        detach()
        super.onDetachedFromWindow()
    }
}
