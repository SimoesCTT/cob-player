package com.cobplayer.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StreamEntry(val url: String, val label: String, val at: Long)

object StreamStore {

    private const val PREFS = "cob_streams"
    private const val KEY = "streams_json"
    private const val MAX = 40

    fun load(ctx: Context): MutableList<StreamEntry> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val out = mutableListOf<StreamEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(StreamEntry(o.getString("url"), o.getString("label"), o.optLong("at")))
        }
        return out
    }

    fun push(ctx: Context, url: String, label: String) {
        val list = load(ctx).filterNot { it.url == url }.toMutableList()
        list.add(0, StreamEntry(url, label, System.currentTimeMillis()))
        while (list.size > MAX) list.removeAt(list.size - 1)
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("url", it.url); put("label", it.label); put("at", it.at)
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun remove(ctx: Context, url: String) {
        val list = load(ctx).filterNot { it.url == url }.toMutableList()
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("url", it.url); put("label", it.label); put("at", it.at)
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
