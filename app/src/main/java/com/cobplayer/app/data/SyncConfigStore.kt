package com.cobplayer.app.data

import android.content.Context

object SyncConfigStore {
    private const val PREFS = "cob_sync"
    private const val KEY_URL = "webdav_url"
    private const val KEY_USER = "webdav_user"
    private const val KEY_LAST_SYNC = "last_sync"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var Context.syncUrl: String
        get() = p(this).getString(KEY_URL, "") ?: ""
        set(v) { p(this).edit().putString(KEY_URL, v).apply() }

    var Context.syncUser: String
        get() = p(this).getString(KEY_USER, "") ?: ""
        set(v) { p(this).edit().putString(KEY_USER, v).apply() }

    // Password goes in SecureStore
    var Context.syncPassword: String
        get() = SecureStore.get(this, "sync_password")
        set(v) { SecureStore.put(this, "sync_password", v) }

    var Context.lastSyncTime: Long
        get() = p(this).getLong(KEY_LAST_SYNC, 0L)
        set(v) { p(this).edit().putLong(KEY_LAST_SYNC, v).apply() }
}
