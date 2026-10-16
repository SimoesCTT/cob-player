package com.cobplayer.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import java.util.concurrent.Executors

object ThumbnailLoader {

    private val cache = LruCache<String, Bitmap>(60)
    private val executor = Executors.newFixedThreadPool(4)

    fun load(ctx: Context, uri: Uri, onResult: (Bitmap?) -> Unit) {
        val key = uri.toString()
        cache.get(key)?.let { onResult(it); return }

        executor.execute {
            val bmp = try {
                if (uri.toString().startsWith("content://media/external/video")) {
                    val id = uri.lastPathSegment?.toLongOrNull() ?: -1L
                    if (id >= 0) MediaStore.Video.Thumbnails.getThumbnail(
                        ctx.contentResolver, id,
                        MediaStore.Video.Thumbnails.MINI_KIND, null
                    ) else null
                } else null
            } catch (e: Exception) { null }

            bmp?.let { cache.put(key, it) }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(bmp)
            }
        }
    }
}
