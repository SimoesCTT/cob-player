package com.cobplayer.app.data

import android.content.Context
import android.net.Uri

object ResumeStore {

    private const val PREFS = "cob_resume"
    private const val MIN_MS = 5000L

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, uri: Uri, positionMs: Long, durationMs: Long) {
        // Do not save if near start or near end (credit roll)
        if (positionMs < MIN_MS) { clear(ctx, uri); return }
        if (durationMs > 0 && positionMs > durationMs - 15_000) {
            clear(ctx, uri); return
        }
        prefs(ctx).edit().putLong(uri.toString(), positionMs).apply()
    }

    fun load(ctx: Context, uri: Uri): Long =
        prefs(ctx).getLong(uri.toString(), 0L)

    fun clear(ctx: Context, uri: Uri) {
        prefs(ctx).edit().remove(uri.toString()).apply()
    }

    fun all(ctx: Context): Map<String, Long> =
        prefs(ctx).all.mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap()
}
