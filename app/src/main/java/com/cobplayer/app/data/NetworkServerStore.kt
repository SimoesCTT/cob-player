package com.cobplayer.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Protocol { SMB, FTP, SFTP }

data class NetworkServer(
    val id: String,
    val label: String,
    val protocol: Protocol,
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val basePath: String
)

object NetworkServerStore {

    private const val PREFS = "cob_network"
    private const val KEY = "servers_json"

    fun load(ctx: Context): MutableList<NetworkServer> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val out = mutableListOf<NetworkServer>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(NetworkServer(
                id = o.getString("id"),
                label = o.getString("label"),
                protocol = Protocol.valueOf(o.getString("protocol")),
                host = o.getString("host"),
                port = o.optInt("port"),
                user = o.optString("user"),
                password = o.optString("password"),
                basePath = o.optString("basePath")
            ))
        }
        return out
    }

    fun save(ctx: Context, list: List<NetworkServer>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("label", s.label)
                put("protocol", s.protocol.name)
                put("host", s.host)
                put("port", s.port)
                put("user", s.user)
                put("password", s.password)
                put("basePath", s.basePath)
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, server: NetworkServer) {
        val list = load(ctx)
        list.removeAll { it.id == server.id }
        list.add(server)
        save(ctx, list)
    }

    fun remove(ctx: Context, id: String) {
        save(ctx, load(ctx).filterNot { it.id == id })
    }
}
