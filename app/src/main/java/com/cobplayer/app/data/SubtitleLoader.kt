package com.cobplayer.app.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes

object SubtitleLoader {

    /** Guesses Media3 MIME type from file extension or fallback. */
    fun guessMime(uri: Uri, fallback: String = MimeTypes.APPLICATION_SUBRIP): String {
        val path = uri.lastPathSegment?.lowercase() ?: return fallback
        return when {
            path.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            path.endsWith(".ass") -> MimeTypes.TEXT_SSA
            path.endsWith(".ssa") -> MimeTypes.TEXT_SSA
            path.endsWith(".ttml") || path.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
            else -> fallback
        }
    }
}
