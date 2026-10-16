package com.cobplayer.app.net

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile

data class FtpEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String,
    val url: String
)

class FtpClientWrapper {

    private var ftp: FTPClient? = null
    private var baseUrl: String = ""

    fun connect(host: String, port: Int, user: String, password: String) {
        disconnect()
        ftp = FTPClient().apply {
            connect(host, port)
            login(user.ifBlank { "anonymous" }, password.ifBlank { "" })
            enterLocalPassiveMode()
        }
        baseUrl = "ftp://$host:$port"
    }

    fun list(path: String): List<FtpEntry> {
        val f = ftp ?: return emptyList()
        val files = f.listFiles(path.ifBlank { "/" }) ?: return emptyList()
        return files.filter { it.name != "." && it.name != ".." }.map {
            val isDir = it.isDirectory
            val child = if (path.endsWith("/")) "$path${it.name}" else "$path/${it.name}"
            FtpEntry(
                name = it.name,
                isDirectory = isDir,
                size = it.size,
                path = child,
                url = "$baseUrl$child"
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun disconnect() {
        try { ftp?.logout() } catch (_: Exception) {}
        try { ftp?.disconnect() } catch (_: Exception) {}
        ftp = null
    }
}
