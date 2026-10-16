package com.cobplayer.app.data

import android.content.Context

object PlayerPrefs {
    private const val PREFS = "cob_player_prefs"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var Context.speed: Float
        get() = prefs(this).getFloat("speed", 1.0f)
        set(v) { prefs(this).edit().putFloat("speed", v).apply() }

    var Context.aspectMode: String
        get() = prefs(this).getString("aspect", "FIT") ?: "FIT"
        set(v) { prefs(this).edit().putString("aspect", v).apply() }

    var Context.repeatMode: Int
        get() = prefs(this).getInt("repeat", 0)
        set(v) { prefs(this).edit().putInt("repeat", v).apply() }

    var Context.shuffle: Boolean
        get() = prefs(this).getBoolean("shuffle", false)
        set(v) { prefs(this).edit().putBoolean("shuffle", v).apply() }

    var Context.subtitleScale: Float
        get() = prefs(this).getFloat("sub_scale", 1.0f)
        set(v) { prefs(this).edit().putFloat("sub_scale", v).apply() }
}
