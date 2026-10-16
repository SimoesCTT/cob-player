package com.cobplayer.app.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object M3u {

    /** Parses #EXTM3U style content. Returns list of (path/url, title). */
    fun parse(ctx: Context, uri: Uri): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var pendingTitle: String? = null

        ctx.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim()
                    when {
                        line.isEmpty() -> {}
                        line.startsWith("#EXTINF", ignoreCase = true) -> {
                            val comma = line.indexOf(',')
                            pendingTitle = if (comma >= 0 && comma < line.length - 1)
                                line.substring(comma + 1).trim() else null
                        }
                        line.startsWith("#") -> {}
                        else -> {
                            val title = pendingTitle
                                ?: line.substringAfterLast('/').substringBeforeLast('.')
                            out.add(line to title)
                            pendingTitle = null
                        }
                    }
                }
            }
        }
        return out
    }

    fun write(ctx: Context, uri: Uri, name: String, uris: List<String>, titles: List<String>) {
        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            OutputStreamWriter(out).use { w ->
                w.write("#EXTM3U\n")
                uris.forEachIndexed { i, u ->
                    val title = titles.getOrNull(i) ?: "Track ${i + 1}"
                    w.write("#EXTINF:-1,$title\n")
                    w.write("$u\n")
                }
            }
        }
    }
}
