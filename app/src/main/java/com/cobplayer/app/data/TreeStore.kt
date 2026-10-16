package com.cobplayer.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri

object TreeStore {
    private const val PREFS = "cob_trees"
    private const val KEY_ROOTS = "roots"

    fun persist(ctx: Context, uri: Uri) {
        ctx.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val roots = load(ctx).toMutableSet()
        roots.add(uri.toString())
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_ROOTS, roots).apply()
    }

    fun load(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_ROOTS, emptySet()) ?: emptySet()

    fun forget(ctx: Context, uri: Uri) {
        val roots = load(ctx).toMutableSet()
        roots.remove(uri.toString())
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_ROOTS, roots).apply()
    }
}
