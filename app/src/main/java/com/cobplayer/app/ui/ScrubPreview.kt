package com.cobplayer.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

/**
 * Generates and caches frame thumbnails for scrubbing preview.
 * Uses MediaMetadataRetriever.getFrameAtTime which works on most formats.
 */
class ScrubPreview(private val context: Context) {

    private val cache = LruCache<String, Bitmap>(80)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun show(uri: Uri, positionMs: Long, target: ImageView, onReady: (() -> Unit)? = null) {
        // Quantize to 1-second buckets so we hit cache often
        val bucket = (positionMs / 1000) * 1000
        val key = "$uri#$bucket"

        cache.get(key)?.let {
            target.setImageBitmap(it)
            onReady?.invoke()
            return
        }

        target.setImageBitmap(null)
        executor.execute {
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                val bmp = mmr.getFrameAtTime(bucket * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                mmr.release()
                if (bmp != null) {
                    cache.put(key, bmp)
                    main.post {
                        target.setImageBitmap(bmp)
                        onReady?.invoke()
                    }
                } else {
                    main.post { onReady?.invoke() }
                }
            } catch (e: Exception) {
                main.post { onReady?.invoke() }
            }
        }
    }

    fun shutdown() {
        executor.shutdown()
    }
}
