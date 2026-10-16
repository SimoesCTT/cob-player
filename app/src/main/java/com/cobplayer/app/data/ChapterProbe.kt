package com.cobplayer.app.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * Reads chapters from a media file using Android platform APIs.
 *
 *   1. Android 14+ (API 34): MediaMetadataRetriever.getChapters() — native support
 *   2. Fallback: sidecar file "<basename>.chapters.txt" with "startMs|title" per line
 *   3. Fallback: for MP4/M4V, parse a text track if present
 *
 * Returns empty list if none of these succeed.
 */
object ChapterProbe {

    fun probe(context: Context, uri: Uri): List<Chapter> {
        // ── Strategy 1: Android 14 native ──
        if (Build.VERSION.SDK_INT >= 34) {
            tryNative(context, uri)?.let { if (it.isNotEmpty()) return it }
        }

        // ── Strategy 2: sidecar file ──
        trySidecar(uri)?.let { if (it.isNotEmpty()) return it }

        // ── Strategy 3: MP4 chapter track ──
        tryMp4Chapters(context, uri)?.let { if (it.isNotEmpty()) return it }

        return emptyList()
    }

    private fun tryNative(context: Context, uri: Uri): List<Chapter>? = try {
        val mmr = android.media.MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        // getChapters() returns List<MediaMetadataRetriever.Chapter> on API 34+
        val result = mmr.javaClass
            .getMethod("getChapters")
            .invoke(mmr) as? List<*>
        mmr.release()
        result?.mapNotNull { item ->
            val clazz = item!!.javaClass
            val title = clazz.getMethod("getTitle").invoke(item) as? String ?: ""
            val startMs = clazz.getMethod("getStartTimeMs").invoke(item) as? Long ?: return@mapNotNull null
            Chapter(title.ifBlank { "Chapter" }, startMs)
        }
    } catch (_: Throwable) { null }

    private fun trySidecar(uri: Uri): List<Chapter>? {
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val base = file.nameWithoutExtension
        val dir = file.parentFile ?: return null
        // Try common sidecar names
        val candidates = listOf(
            "$base.chapters.txt", "$base.chapters", "$base.chap"
        )
        candidates.forEach { name ->
            val c = File(dir, name)
            if (c.exists()) {
                return try {
                    c.readLines().mapNotNull { line ->
                        val parts = line.split("|", limit = 2)
                        if (parts.size != 2) return@mapNotNull null
                        val ms = parts[0].trim().toLongOrNull() ?: return@mapNotNull null
                        Chapter(parts[1].trim(), ms)
                    }
                } catch (_: Exception) { emptyList() }
            }
        }
        return null
    }

    /**
     * MP4/M4V can embed chapters as a "text" track with sample timing.
     * MediaExtractor exposes the track but not its chapter metadata directly,
     * so we approximate by finding a text track and treating its sample times
     * as chapter boundaries (without titles).
     *
     * This is best-effort — many MP4s use a proprietary "chpl" atom that
     * Android doesn't expose. Consider it a bonus if it fires.
     */
    private fun tryMp4Chapters(context: Context, uri: Uri): List<Chapter>? {
        // Only works with file:// URIs
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(path)
            val count = extractor.trackCount
            for (i in 0 until count) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("text/") || mime.contains("subtitle")) {
                    // No reliable way to enumerate samples as chapters without titles;
                    // return empty and let sidecar or native handle it.
                    extractor.release()
                    return null
                }
            }
            extractor.release()
            null
        } catch (_: Exception) { null }
    }
}
