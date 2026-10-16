package com.cobplayer.app.data

import android.content.Context

object SettingsStore {
    private const val PREFS = "cob_settings"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var Context.showThumbnails: Boolean
        get() = p(this).getBoolean("thumbs", true)
        set(v) { p(this).edit().putBoolean("thumbs", v).apply() }

    var Context.showHiddenFiles: Boolean
        get() = p(this).getBoolean("hidden", false)
        set(v) { p(this).edit().putBoolean("hidden", v).apply() }

    var Context.sortMode: String
        get() = p(this).getString("sort", "date_desc") ?: "date_desc"
        set(v) { p(this).edit().putString("sort", v).apply() }

    var Context.sleepTimerDefaultMin: Int
        get() = p(this).getInt("sleep_min", 30)
        set(v) { p(this).edit().putInt("sleep_min", v).apply() }

    var Context.seekSeconds: Int
        get() = p(this).getInt("seek_sec", 10)
        set(v) { p(this).edit().putInt("seek_sec", v).apply() }

    var Context.groupByFolder: Boolean
        get() = p(this).getBoolean("group_folder", false)
        set(v) { p(this).edit().putBoolean("group_folder", v).apply() }

    var Context.gridView: Boolean
        get() = p(this).getBoolean("grid", false)
        set(v) { p(this).edit().putBoolean("grid", v).apply() }

    var Context.subtitleScale: Float
        get() = p(this).getFloat("sub_scale", 1.0f)
        set(v) { p(this).edit().putFloat("sub_scale", v).apply() }

    var Context.lastFolderRoot: String
        get() = p(this).getString("last_root", "") ?: ""
        set(v) { p(this).edit().putString("last_root", v).apply() }
}
