package com.cobplayer.app.data

import android.content.Context
import android.net.Uri
import java.io.File

object AutoSubtitle {

    private val SUB_EXTS = listOf(".srt", ".ass", ".ssa", ".vtt", ".sub")

    /**
     * Given a video Uri, find a subtitle file with the same base name
     * in the same folder. Returns null if nothing found.
     */
    fun findFor(context: Context, videoUri: Uri): Uri? {
        // Only works with file:// URIs (direct filesystem)
        val path = when (videoUri.scheme) {
            "file" -> videoUri.path
            else -> return null
        } ?: return null

        val videoFile = File(path)
        if (!videoFile.exists()) return null
        val dir = videoFile.parentFile ?: return null

        val base = videoFile.nameWithoutExtension
        val candidates = listOf(
            "$base.srt", "$base.ass", "$base.ssa", "$base.vtt", "$base.sub"
        )

        // Exact base-name match first
        candidates.forEach { name ->
            val f = File(dir, name)
            if (f.exists()) return Uri.fromFile(f)
        }

        // Fall back: any subtitle file sharing the same prefix (case-insensitive)
        val baseLower = base.lowercase()
        dir.listFiles()?.forEach { f ->
            val nameLower = f.name.lowercase()
            if (SUB_EXTS.any { nameLower.endsWith(it) } &&
                nameLower.startsWith(baseLower)
            ) {
                return Uri.fromFile(f)
            }
        }
        return null
    }
}
