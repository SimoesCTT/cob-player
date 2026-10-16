package com.cobplayer.app.data

import android.content.Context
import android.net.Uri

data class Chapter(val title: String, val startMs: Long)

object ChapterStore {
    fun extract(context: Context, uri: Uri): List<Chapter> {
        return try {
            ChapterProbe.probe(context, uri)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
