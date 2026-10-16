package com.cobplayer.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val uris: MutableList<String> = mutableListOf(),
    val titles: MutableList<String> = mutableListOf()
)

object PlaylistStore {

    private const val PREFS = "cob_playlists"
    private const val KEY = "playlists_json"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Playlist> {
        val raw = prefs(ctx).getString(KEY, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val out = mutableListOf<Playlist>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pl = Playlist(o.getString("id"), o.getString("name"))
            val u = o.optJSONArray("uris") ?: JSONArray()
            val t = o.optJSONArray("titles") ?: JSONArray()
            for (j in 0 until u.length()) pl.uris.add(u.getString(j))
            for (j in 0 until t.length()) pl.titles.add(t.getString(j))
            out.add(pl)
        }
        return out
    }

    fun save(ctx: Context, playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val o = JSONObject()
            o.put("id", pl.id)
            o.put("name", pl.name)
            o.put("uris", JSONArray(pl.uris))
            o.put("titles", JSONArray(pl.titles))
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun create(ctx: Context, name: String): Playlist {
        val list = load(ctx)
        val pl = Playlist(id = System.currentTimeMillis().toString(), name = name)
        list.add(pl)
        save(ctx, list)
        return pl
    }

    fun addItem(ctx: Context, playlistId: String, uri: Uri, title: String) {
        val list = load(ctx)
        val pl = list.find { it.id == playlistId } ?: return
        if (!pl.uris.contains(uri.toString())) {
            pl.uris.add(uri.toString())
            pl.titles.add(title)
        }
        save(ctx, list)
    }

    fun removeItem(ctx: Context, playlistId: String, uri: Uri) {
        val list = load(ctx)
        val pl = list.find { it.id == playlistId } ?: return
        val idx = pl.uris.indexOf(uri.toString())
        if (idx >= 0) { pl.uris.removeAt(idx); pl.titles.removeAt(idx) }
        save(ctx, list)
    }

    fun rename(ctx: Context, playlistId: String, newName: String) {
        val list = load(ctx)
        val pl = list.find { it.id == playlistId } ?: return
        val idx = list.indexOf(pl)
        list[idx] = pl.copy(name = newName)
        save(ctx, list)
    }

    fun delete(ctx: Context, playlistId: String) {
        save(ctx, load(ctx).filterNot { it.id == playlistId })
    }
}
