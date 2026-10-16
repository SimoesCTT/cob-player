package com.cobplayer.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class RecentEntry(val uri: String, val title: String, val at: Long)

object RecentStore {

    private const val PREFS = "cob_recent"
    private const val KEY = "recent_json"
    private const val MAX = 50

    fun load(ctx: Context): MutableList<RecentEntry> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val out = mutableListOf<RecentEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(RecentEntry(o.getString("uri"), o.getString("title"), o.getLong("at")))
        }
        return out
    }

    fun push(ctx: Context, uri: Uri, title: String) {
        val list = load(ctx).filterNot { it.uri == uri.toString() }.toMutableList()
        list.add(0, RecentEntry(uri.toString(), title, System.currentTimeMillis()))
        while (list.size > MAX) list.removeAt(list.size - 1)
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("uri", it.uri); put("title", it.title); put("at", it.at)
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
