package com.cobplayer.app.net

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny WebDAV client for a single file — enough to push/pull a snapshot JSON.
 * Works with Nextcloud, ownCloud, Synology, Seafile, most NAS WebDAV servers.
 */
object WebDavClient {

    data class Config(
        val baseUrl: String,
        val username: String,
        val password: String,
        val filename: String = "snapshot.json"
    )

    private fun open(method: String, cfg: Config): HttpURLConnection {
        val url = URL(cfg.baseUrl.trimEnd('/') + "/" + cfg.filename)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        val auth = Base64.encodeToString(
            "${cfg.username}:${cfg.password}".toByteArray(),
            Base64.NO_WRAP
        )
        conn.setRequestProperty("Authorization", "Basic $auth")
        conn.setRequestProperty("User-Agent", "CobPlayer/1.0")
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        return conn
    }

    private fun is2xx(code: Int): Boolean = code >= 200 && code < 300

    fun upload(cfg: Config, json: String): Boolean {
        return try {
            val conn = open("PUT", cfg)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            val code = conn.responseCode
            conn.disconnect()
            is2xx(code)
        } catch (_: Exception) {
            false
        }
    }

    fun download(cfg: Config): String? {
        return try {
            val conn = open("GET", cfg)
            val code = conn.responseCode
            if (!is2xx(code)) {
                conn.disconnect()
                return null
            }
            val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()
            text
        } catch (_: Exception) {
            null
        }
    }

    fun test(cfg: Config): Pair<Boolean, String> {
        return try {
            val conn = open("PROPFIND", cfg)
            conn.setRequestProperty("Depth", "0")
            val code = conn.responseCode
            conn.disconnect()
            val ok = is2xx(code)
            Pair(ok, "HTTP $code")
        } catch (e: Exception) {
            Pair(false, e.message ?: "unknown")
        }
    }
}
