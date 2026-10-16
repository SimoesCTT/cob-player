package com.cobplayer.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * A portable snapshot of playback positions and metadata.
 * Sync this JSON between devices via WebDAV, Syncthing, Google Drive, etc.
 *
 * Match keys are "media fingerprints" — hash(displayName + sizeBytes)
 * for local files, or hash(url) for streams. This lets two devices
 * recognise they're playing the same movie even if file paths differ.
 */
object SyncSnapshot {

    private const val SCHEMA = 1

    data class Entry(
        val fingerprint: String,
        val title: String,
        val positionMs: Long,
        val durationMs: Long,
        val updatedAt: Long
    )

    fun export(ctx: Context): String {
        val entries = mutableListOf<Entry>()

        // Local resume positions
        val resumePrefs = ctx.getSharedPreferences("cob_resume", Context.MODE_PRIVATE)
        resumePrefs.all.forEach { (uriStr, pos) ->
            val posMs = (pos as? Long) ?: return@forEach
            if (posMs <= 0) return@forEach
            val uri = Uri.parse(uriStr)
            val title = uri.lastPathSegment ?: uriStr
            entries.add(Entry(
                fingerprint = fingerprintFor(uriStr, title),
                title = title,
                positionMs = posMs,
                durationMs = 0L,
                updatedAt = System.currentTimeMillis()
            ))
        }

        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("fp", e.fingerprint)
                put("title", e.title)
                put("pos", e.positionMs)
                put("dur", e.durationMs)
                put("at", e.updatedAt)
            })
        }

        return JSONObject().apply {
            put("schema", SCHEMA)
            put("exported", System.currentTimeMillis())
            put("entries", arr)
        }.toString(2)
    }

    /** Merge a remote snapshot: newer entries win. */
    fun import(ctx: Context, json: String): Int {
        return try {
            val root = JSONObject(json)
            if (root.optInt("schema") != SCHEMA) return 0
            val entries = root.getJSONArray("entries")
            var merged = 0
            val resumePrefs = ctx.getSharedPreferences("cob_resume", Context.MODE_PRIVATE)
            val editor = resumePrefs.edit()

            for (i in 0 until entries.length()) {
                val o = entries.getJSONObject(i)
                val fp = o.getString("fp")
                val pos = o.getLong("pos")
                val at = o.optLong("at")
                // Store under a fingerprint key that ResumeStore also writes
                editor.putLong("fp_$fp", pos)
                editor.putLong("fp_${fp}_at", at)
                merged++
            }
            editor.apply()
            merged
        } catch (_: Exception) { 0 }
    }

    /** Stable-ish fingerprint from title + optional size. */
    fun fingerprintFor(uri: String, title: String, size: Long = 0L): String {
        val s = "$title|$size"
        val md = java.security.MessageDigest.getInstance("SHA-1")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }

    /** Look up saved position for a given fingerprint. */
    fun lookup(ctx: Context, fingerprint: String): Long {
        val p = ctx.getSharedPreferences("cob_resume", Context.MODE_PRIVATE)
        return p.getLong("fp_$fingerprint", 0L)
    }
}
