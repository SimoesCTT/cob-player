package com.cobplayer.app.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.os.Handler
import android.os.Looper

object Snapshotter {

    fun capture(view: View, onDone: (Uri?) -> Unit) {
        if (view is SurfaceView && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val handler = Handler(Looper.getMainLooper())
            try {
                PixelCopy.request(view, bmp, { result ->
                    if (result == PixelCopy.SUCCESS) {
                        val uri = save(view.context, bmp)
                        onDone(uri)
                    } else onDone(null)
                }, handler)
            } catch (e: Exception) {
                onDone(null)
            }
        } else {
            // TextureView fallback
            val bmp = try {
                if (view is android.view.TextureView)
                    view.bitmap
                else null
            } catch (_: Exception) { null }
            onDone(bmp?.let { save(view.context, it) })
        }
    }

    fun save(context: Context, bitmap: Bitmap): Uri? {
        val name = "cob_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Cob Player")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            return uri
        }

        val dir = java.io.File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Cob Player"
        ).apply { mkdirs() }
        val file = java.io.File(dir, name)
        return try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) { null }
    }
}
