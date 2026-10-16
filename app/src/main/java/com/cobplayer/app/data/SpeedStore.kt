package com.cobplayer.app.data

import android.content.Context
import android.net.Uri

object SpeedStore {
    private const val PREFS = "cob_speed"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, uri: Uri, speed: Float) {
        prefs(ctx).edit().putFloat(uri.toString(), speed).apply()
    }

    fun load(ctx: Context, uri: Uri): Float =
        prefs(ctx).getFloat(uri.toString(), 1.0f)

    fun clear(ctx: Context, uri: Uri) {
        prefs(ctx).edit().remove(uri.toString()).apply()
    }
}
